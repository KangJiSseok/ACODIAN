package com.ibank.axwms.domain.organization.user.service;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.entity.Team;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserTeamRepository userTeamRepository;
    private final TeamRepository teamRepository;

    /**
     * access token principal 에 해당하는 현재 로그인 사용자의 프로필/소속 팀 문맥을 조회한다.
     * 프론트엔드가 앱 초기화 시 한 번의 호출로 사용자 기본 정보와 팀 컨텍스트를 확보할 수 있도록 필요한 필드를 조립해 반환한다.
     */
    public GetMyProfileApiDto.Response getMyProfile(CustomUserPrincipal principal) {
        User user = getUserOrThrow(principal.userId());
        Department department = getRequiredDepartment(user.getDepartmentId());
        List<GetMyProfileApiDto.Response.TeamSummary> teams = getTeamSummaries(user.getId());

        return GetMyProfileApiDto.Response.of(user, department, teams);
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
     * JWT subject 로 복원한 사용자 id 에 해당하는 User 를 조회한다.
     * access token 은 유효하지만 사용자가 삭제된 비정상 케이스를 404 비즈니스 예외로 정규화한다.
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자의 소속 부서명을 응답에 포함하기 위해 Department 를 조회한다.
     * 사용자 레코드가 가리키는 부서가 없으면 조직 문맥이 깨진 상태이므로 사용자 미존재와 동일하게 취급한다.
     */
    private Department getRequiredDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자-팀 관계 순서를 유지한 채 ACTIVE membership 기준 팀 요약 목록을 만든다.
     * soft-delete 된 팀이나 LEFT membership 은 현재 사용자 문맥에서 노출하지 않는다.
     */
    private List<GetMyProfileApiDto.Response.TeamSummary> getTeamSummaries(Long userId) {
        List<UserTeam> userTeams = userTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(userId, UserTeamStatus.ACTIVE);
        if (userTeams.isEmpty()) {
            return List.of();
        }

        Map<Long, Team> teamsById = teamRepository.findAllById(
                        userTeams.stream()
                                .map(UserTeam::getTeamId)
                                .toList()
                ).stream()
                .filter(team -> team.getDeletedAt() == null)
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        return userTeams.stream()
                .map(userTeam -> toTeamSummary(userTeam, teamsById.get(userTeam.getTeamId())))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 사용자-팀 관계와 팀 엔티티를 응답용 팀 요약으로 변환한다.
     * FK 무결성상 team 은 존재해야 하지만, 운영 데이터 불일치가 있더라도 전체 조회를 500 으로 깨지 않게 누락 팀은 제외한다.
     */
    private GetMyProfileApiDto.Response.TeamSummary toTeamSummary(UserTeam userTeam, Team team) {
        if (team == null) {
            return null;
        }
        return new GetMyProfileApiDto.Response.TeamSummary(
                userTeam.getIsPrimary(),
                team.getId(),
                team.getTeamName(),
                userTeam.getIsLeader(),
                userTeam.getTeamRole(),
                userTeam.getAllocation()
        );
    }
}
