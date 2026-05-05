package com.ibank.axwms.domain.organization.user.service;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;

    /**
     * access token principal 에 해당하는 현재 로그인 사용자의 프로필/소속 팀 문맥을 조회한다.
     * 프론트엔드가 앱 초기화 시 한 번의 호출로 사용자 기본 정보와 팀 컨텍스트를 확보할 수 있도록 필요한 필드를 조립해 반환한다.
     */
    public GetMyProfileApiDto.Response getMyProfile(CustomUserPrincipal principal) {
        User user = getUserOrThrow(principal.userId());
        Department department = getDepartmentOrThrow(user.getDepartmentId());
        List<GetMyProfileApiDto.Response.TeamSummary> teams = getTeamSummaries(user.getId());

        return GetMyProfileApiDto.Response.of(user, department, teams);
    }

    /**
     * 사용자 id 에 해당하는 사용자 상세와 전체 ACTIVE 팀 membership 문맥을 조회한다.
     *
     * @param userId 조회 대상 사용자 id
     * @return 사용자 기본 정보, 부서 정보, 소속 팀 목록
     * @throws BusinessException USER_NOT_FOUND 사용자가 없거나 사용자 부서 문맥이 깨진 경우
     */
    public GetUserApiDto.Response getUser(Long userId) {
        User user = getUserOrThrow(userId);
        Department department = getDepartmentOrThrow(user.getDepartmentId());
        List<GetUserApiDto.Response.TeamSummary> teams = getUserDetailTeamSummaries(user.getId());

        return GetUserApiDto.Response.of(user, department, teams);
    }

    /**
     * 인증된 사용자 role 기준으로 상위 관리자 지정 후보를 조회한다.
     * DIRECTOR 는 부서장 전체를 선택할 수 있고, DEPT_HEAD 는 자기 자신만 후보로 노출한다.
     */
    public List<GetAdminCandidatesApiDto.Response> getAdminCandidates(CustomUserPrincipal principal) {
        if (UserRole.DIRECTOR.name().equals(principal.roleCode())) {
            return userRepository.findAllByRoleCodeOrderByIdAsc(UserRole.DEPT_HEAD).stream()
                    .map(GetAdminCandidatesApiDto.Response::from)
                    .toList();
        }

        return List.of(GetAdminCandidatesApiDto.Response.from(getUserOrThrow(principal.userId())));
    }

    /**
     * 사용자 목록을 페이지네이션 없이 필터 조건과 role 고정 정렬 기준으로 조회한다.
     * 인증과 role gate 는 Controller/Security 체인이 보장하므로 목록 조회는 요청 filter 만 repository query 로 정규화한다.
     */
    public List<GetUsersApiDto.Response> getUsers(GetUsersApiDto.Request request) {
        return userRepository.findUsers(UserListQuery.from(request)).stream()
                .map(GetUsersApiDto.Response::from)
                .toList();
    }

    /**
     * 사용자 기본 정보와 대표 소속 팀을 부분 수정한다.
     *
     * @param principal 수정 요청을 보낸 인증 사용자
     * @param userId 수정 대상 사용자 id
     * @param request null 이 아닌 필드만 반영할 부분 수정 요청
     * @throws BusinessException USER_NOT_FOUND 수정 대상 사용자가 없을 때
     * @throws BusinessException AUTH_ACCESS_DENIED DEPT_HEAD 가 수정 가능한 부서/role 범위를 벗어났을 때
     * @throws BusinessException DEPARTMENT_NOT_FOUND 요청 부서가 없을 때
     * @throws BusinessException TEAM_NOT_FOUND 요청 팀이 없거나 대상 사용자의 membership 이 아닐 때
     */
    @Transactional
    public void updateUser(CustomUserPrincipal principal, Long userId, UpdateUserApiDto.Request request) {
        User user = getUserOrThrow(userId);
        validateUpdateUserPermission(principal, user, request);
        ensureDepartmentExistsOrThrow(request.departmentId());
        updatePrimaryTeam(userId, request.primaryTeamId());

        user.updatePartial(
                request.userName(),
                request.email(),
                request.profileImageUrl(),
                request.positionName(),
                request.titleName(),
                request.departmentId(),
                request.phone(),
                request.employmentStatus(),
                request.joinDate()
        );
    }

    /** DIRECTOR는 모두 평가 할 수 있다.
     * DEPT_HEAD 는 자기 부서의 TEAM_LEAD 또는 MEMBER 만 수정할 수 있다. */
    private void validateUpdateUserPermission(CustomUserPrincipal principal,
                                              User targetUser,
                                              UpdateUserApiDto.Request request) {
        // DIRECTOR는 return
        if (UserRole.DIRECTOR.name().equals(principal.roleCode())) {
            return;
        }

        User actor = getUserOrThrow(principal.userId());
        boolean targetBelongsToSameDepartment = actor.getDepartmentId().equals(targetUser.getDepartmentId());
        boolean targetRoleEditable = targetUser.getRoleCode() == UserRole.TEAM_LEAD
                || targetUser.getRoleCode() == UserRole.MEMBER;

        // request 오는 변경하려는 부서가 자기랑 같거나, null일때. 즉 DEPT_HEAD는 타부서로 변경 못한다.
        boolean requestedDepartmentStaysInScope = request.departmentId() == null
                || actor.getDepartmentId().equals(request.departmentId());

        if (!targetBelongsToSameDepartment || !targetRoleEditable || !requestedDepartmentStaysInScope) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    /**
     * 사용자 id 에 해당하는 사용자의 소속 부서 id 를 조회한다.
     * 다른 모듈(worklog 가시 범위 정책 등) 이 organization 모듈을 service 경계로 우회하기 위한 진입점이다.
     *
     * @param userId 조회 대상 사용자 id
     * @return 사용자가 소속된 부서 id
     * @throws BusinessException USER_NOT_FOUND 사용자가 없거나 access token 문맥이 복원 불가일 때
     */
    public Long getDepartmentIdOrThrow(Long userId) {
        return getUserOrThrow(userId).getDepartmentId();
    }

    /**
     * 사용자 id 에 해당하는 User 를 조회한다. 없으면 404에러를 반환한다.
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자에 연결된 departmentId 의 Department 문맥을 조회한다. 없으면 사용자 문맥 복원 실패로 처리한다.
     */
    private Department getDepartmentOrThrow(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** 요청 부서 id 가 null 이 아니면 실제 존재 여부를 검증한다. */
    private void ensureDepartmentExistsOrThrow(Long departmentId) {
        if (departmentId != null && !departmentRepository.existsById(departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
    }

    /** 대표 소속 팀 요청이 있으면 기존 대표 플래그를 모두 해제하고 요청 membership 만 대표로 둔다. */
    private void updatePrimaryTeam(Long userId, Long primaryTeamId) {
        if (primaryTeamId == null) {
            return;
        }
        if (!teamRepository.existsByIdAndDeletedAtIsNull(primaryTeamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        userTeamRepository.findByUserIdAndTeamId(userId, primaryTeamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        for (UserTeam membership : userTeamRepository.findAllByUserId(userId)) {
            if (primaryTeamId.equals(membership.getTeamId())) {
                membership.markAsPrimary();
            } else {
                membership.markAsSecondary();
            }
        }
    }

    /**
     * 사용자-팀 관계 순서를 유지한 채 ACTIVE membership 기준 팀 요약 목록을 만든다.
     * soft-delete 된 팀이나 LEFT membership 은 현재 사용자 문맥에서 노출하지 않는다.
     */
    private List<GetMyProfileApiDto.Response.TeamSummary> getTeamSummaries(Long userId) {
        return fetchUserTeamSummaries(userId).stream()
                .map(p -> new GetMyProfileApiDto.Response.TeamSummary(
                        p.isPrimary(),
                        p.teamId(),
                        p.teamName(),
                        p.isLeader(),
                        p.teamRole(),
                        p.allocation()
                ))
                .toList();
    }

    /**
     * 사용자 상세 응답의 팀 membership 을 대표 소속 여부, 팀 대표 여부 순서로 정렬해 만든다.
     * LEFT membership 과 soft-delete 된 팀은 현재 소속 문맥에서 제외한다.
     */
    private List<GetUserApiDto.Response.TeamSummary> getUserDetailTeamSummaries(Long userId) {
        return fetchUserTeamSummaries(userId).stream()
                .map(p -> new GetUserApiDto.Response.TeamSummary(
                        p.isPrimary(),
                        p.teamId(),
                        p.teamName(),
                        p.isLeader(),
                        p.teamRole()
                ))
                .toList();
    }

    /** 사용자의 ACTIVE membership 중 soft-delete 되지 않은 팀을 주 소속·리더 순으로 조회한다. */
    private List<UserTeamSummaryProjection> fetchUserTeamSummaries(Long userId) {
        return userTeamRepository.findUserTeamSummaries(userId);
    }
}
