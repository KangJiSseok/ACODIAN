package com.ibank.axwms.domain.organization.user.service;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.DepartmentRepository;
import com.ibank.axwms.domain.organization.department.service.DepartmentService;
import com.ibank.axwms.domain.organization.team.entity.UserTeam;
import com.ibank.axwms.domain.organization.team.repository.TeamRepository;
import com.ibank.axwms.domain.organization.team.repository.UserTeamRepository;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.dto.GetAdminCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetDepartmentCandidatesApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUserApiDto;
import com.ibank.axwms.domain.organization.user.dto.GetUsersApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateMyProfileApiDto;
import com.ibank.axwms.domain.organization.user.dto.UpdateUserApiDto;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.event.ProfileImageCommittedEvent;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.FinalUploadResult;
import com.ibank.axwms.domain.organization.user.service.ProfileImageStorageService.TempUploadResult;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final ProfileImageStorageService profileImageStorageService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * access token principal 에 해당하는 현재 로그인 사용자의 프로필/소속 팀 문맥을 조회한다.
     * 프론트엔드가 앱 초기화 시 한 번의 호출로 사용자 기본 정보와 팀 컨텍스트를 확보할 수 있도록 필요한 필드를 조립해 반환한다.
     */
    public GetMyProfileApiDto.Response getMyProfile(CustomUserPrincipal principal) {
        User user = getUserOrThrow(principal.userId());
        Department department = user.getDepartmentId() != null
                ? getDepartmentOrThrow(user.getDepartmentId())
                : null;
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
        Department department = user.getDepartmentId() != null
                ? getDepartmentOrThrow(user.getDepartmentId())
                : null;
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
     * 아직 부서에 소속되지 않은 DEPT_HEAD 사용자를 부서 배정 후보로 조회한다.
     * 후보 조건은 사용자 역할과 departmentId null 여부만 사용하며, 호출 권한은 Controller role gate 가 보장한다.
     */
    public List<GetDepartmentCandidatesApiDto.Response> getDepartmentCandidates() {
        return userRepository.findAllByRoleCodeAndDepartmentIdIsNullOrderByIdAsc(UserRole.DEPT_HEAD).stream()
                .map(GetDepartmentCandidatesApiDto.Response::from)
                .toList();
    }

    /**
     * 사용자 목록을 필터 조건, role 고정 정렬 기준, 페이지 요청 기준으로 조회한다.
     * 인증과 role gate 는 Controller/Security 체인이 보장하므로 목록 조회는 요청 filter 만 repository query 로 정규화한다.
     */
    public PageResponse<GetUsersApiDto.Response> getUsers(GetUsersApiDto.Request request) {
        return GetUsersApiDto.Response.fromPage(userRepository.findUsers(UserListQuery.from(request)));
    }

    /**
     * 현재 로그인 사용자 본인의 프로필을 principal.userId 경계 안에서만 부분 수정한다.
     *
     * @param principal 수정 대상 사용자를 고정하는 인증 사용자
     * @param request null 이 아닌 필드만 반영할 self 부분 수정 요청
     * @param profileImage 새 프로필 이미지 파일. null 또는 empty 이면 기존 이미지를 유지한다.
     * @throws BusinessException USER_NOT_FOUND principal 사용자가 없을 때
     * @throws BusinessException DEPARTMENT_NOT_FOUND 요청 부서가 없거나 활성 상태가 아닐 때
     * @throws BusinessException USER_INVALID_TITLE_NAME 지원하지 않는 직책명을 요청했을 때
     * @throws BusinessException USER_PROFILE_IMAGE_UPLOAD_FAILED final 이미지 업로드에 실패했을 때
     */
    @Transactional
    public void updateMyProfile(CustomUserPrincipal principal,
                                UpdateMyProfileApiDto.Request request,
                                MultipartFile profileImage) {
        User user = getUserOrThrow(principal.userId());
        validateActiveDepartmentIfPresent(request.departmentId());
        UserRole roleCode = resolveSelfRoleCode(request.titleName());

        FinalUploadResult finalUpload = uploadFinalProfileImage(profileImage);
        String oldProfileImageKey = finalUpload != null
                ? profileImageStorageService.resolveDeletableProfileImageKey(user.getProfileImageUrl())
                : null;
        if (finalUpload != null) {
            registerFinalProfileImageSynchronization(finalUpload.finalKey(), oldProfileImageKey);
        }

        user.updateMyProfile(
                request.departmentId(),
                request.userName(),
                request.email(),
                finalUpload != null ? finalUpload.finalUrl() : null,
                request.positionName(),
                request.titleName(),
                roleCode,
                request.joinDate(),
                request.phone(),
                request.employmentStatus()
        );
        userRepository.flush();
    }

    /**
     * 사용자 기본 정보와 대표 소속 팀을 부분 수정한다.
     *
     * @param principal 수정 요청을 보낸 인증 사용자
     * @param userId 수정 대상 사용자 id
     * @param request null 이 아닌 필드만 반영할 부분 수정 요청
     * @param profileImage 새 프로필 이미지 파일. null 또는 empty 이면 기존 이미지를 유지한다.
     * @throws BusinessException USER_NOT_FOUND 수정 대상 사용자가 없을 때
     * @throws BusinessException AUTH_ACCESS_DENIED DEPT_HEAD 가 수정 가능한 부서/role 범위를 벗어났을 때
     * @throws BusinessException DEPARTMENT_NOT_FOUND 요청 부서가 없을 때
     * @throws BusinessException TEAM_NOT_FOUND 요청 팀이 없거나 대상 사용자의 membership 이 아닐 때
     */
    @Transactional
    public void updateUser(CustomUserPrincipal principal,
                           Long userId,
                           UpdateUserApiDto.Request request,
                           MultipartFile profileImage) {
        User user = getUserOrThrow(userId);
        validateUpdateUserPermission(principal, user, request);
        ensureDepartmentExistsOrThrow(request.departmentId());
        updatePrimaryTeam(userId, request.primaryTeamId());

        TempUploadResult tempUpload = null;
        //S3 temp에 올려놓기
        if (profileImage != null && !profileImage.isEmpty()) {
            tempUpload = profileImageStorageService.uploadTemp(profileImage);
        }

        String oldProfileImageKey = tempUpload != null
                ? profileImageStorageService.resolveDeletableProfileImageKey(user.getProfileImageUrl())
                : null;
        String profileImageUrl = tempUpload != null ? tempUpload.finalUrl() : null;

        user.updatePartial(
                request.userName(),
                request.email(),
                profileImageUrl,
                request.positionName(),
                request.titleName(),
                request.departmentId(),
                request.phone(),
                request.employmentStatus(),
                request.joinDate()
        );

        if (tempUpload != null) {
            eventPublisher.publishEvent(new ProfileImageCommittedEvent(
                    tempUpload.tempKey(),
                    tempUpload.finalKey(),
                    oldProfileImageKey
            ));
        }
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
        boolean targetBelongsToSameDepartment = actor.getDepartmentId() != null
                && actor.getDepartmentId().equals(targetUser.getDepartmentId());
        boolean targetRoleEditable = targetUser.getRoleCode() == UserRole.TEAM_LEAD
                || targetUser.getRoleCode() == UserRole.MEMBER;

        // request 오는 변경하려는 부서가 자기랑 같거나, null일때. 즉 DEPT_HEAD는 타부서로 변경 못한다.
        boolean requestedDepartmentStaysInScope = request.departmentId() == null
                || (actor.getDepartmentId() != null && actor.getDepartmentId().equals(request.departmentId()));

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

    /** 부서 변경은 활성 부서 정책으로만 통과시킨다. */
    private void validateActiveDepartmentIfPresent(Long departmentId) {
        if (departmentId != null) {
            departmentService.validateActiveDepartment(departmentId);
        }
    }

    /** self titleName 이 실제 role 매핑을 바꾸는 요청일 때만 UserRole 로 해석하고, 실패하면 self 전용 코드로 차단한다. */
    private UserRole resolveSelfRoleCode(String titleName) {
        if (!StringUtils.hasText(titleName)) {
            return null;
        }
        return UserRole.findByTitleName(titleName)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_INVALID_TITLE_NAME));
    }

    /** 파일이 있을 때만 final 직접 업로드를 수행해 self API 의 ADR-021 예외 수명주기를 시작한다. */
    private FinalUploadResult uploadFinalProfileImage(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }
        return profileImageStorageService.uploadFinal(profileImage);
    }

    /** 새 final object 는 rollback 때 보상 삭제하고 기존 object 는 commit 성공 뒤에만 best-effort 삭제한다.
     *  트랜잭션 성공 전에는 기존 이미지를 절대 지우지 않고, 실패하면 새 이미지를 정리 */
    private void registerFinalProfileImageSynchronization(String newFinalKey, String oldProfileImageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            // 트랜잭션 성공하면 S3에 있는 이전 이미지 삭제
            @Override
            public void afterCommit() {
                if (!newFinalKey.equals(oldProfileImageKey)) {
                    profileImageStorageService.deleteBestEffort(oldProfileImageKey);
                }
            }

            // 트랜잭션이 실패해서 롤백해야한다면, S3에 업로드한 파일 삭제
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    profileImageStorageService.deleteBestEffort(newFinalKey);
                }
            }
        });
    }

    /** 대표 소속 팀 요청이 있으면 DB 유니크 제약과 충돌하지 않도록 기존 대표 해제를 먼저 확정한 뒤 요청 membership 만 대표로 둔다. */
    private void updatePrimaryTeam(Long userId, Long primaryTeamId) {
        if (primaryTeamId == null) {
            return;
        }
        if (!teamRepository.existsByIdAndDeletedAtIsNull(primaryTeamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }

        List<UserTeam> memberships = userTeamRepository.findAllByUserId(userId);
        UserTeam requestedMembership = memberships.stream()
                .filter(membership -> primaryTeamId.equals(membership.getTeamId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));


        boolean requestedAlreadyPrimary = Boolean.TRUE.equals(requestedMembership.getIsPrimary());

        // 내가 소속한 기존 대표팀이 있으면 해제
        boolean clearedOtherPrimary = false;
        for (UserTeam membership : memberships) {
            if (!primaryTeamId.equals(membership.getTeamId()) && Boolean.TRUE.equals(membership.getIsPrimary())) {
                membership.markAsSecondary();
                clearedOtherPrimary = true;
            }
        }
        if (clearedOtherPrimary && !requestedAlreadyPrimary) {
            userTeamRepository.flush();
        }
        requestedMembership.markAsPrimary();
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
