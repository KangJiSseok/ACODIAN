package com.ibank.axwms.domain.organization.evaluation.service;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
import com.ibank.axwms.domain.organization.evaluation.repository.UserEvaluationRepository;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserEvaluationService {

    private final UserEvaluationRepository userEvaluationRepository;
    private final UserRepository userRepository;

    /** 현재 로그인 사용자의 역할/부서 visible scope 에 맞춰 특정 사용자의 평가 이력을 조회한다. */
    public PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> getUserEvaluations(
            CustomUserPrincipal principal,
            Long userId,
            GetUserEvaluationsApiDto.Request request
    ) {
        User targetUser = getUserOrThrow(userId);
        validateEvaluationReadAccess(principal, targetUser);

        UserEvaluationPageQuery query = UserEvaluationPageQuery.of(userId, request);
        return GetUserEvaluationsApiDto.EvaluationSummary.fromPage(userEvaluationRepository.findEvaluationPage(query));
    }

    /** 특정 사용자에게 평가를 등록하며, 조직장 역할과 대상 사용자 범위를 저장 전에 고정한다. */
    @Transactional
    public void createUserEvaluation(
            CustomUserPrincipal principal,
            Long userId,
            CreateUserEvaluationApiDto.Request request
    ) {
        User targetUser = getUserOrThrow(userId);
        validateEvaluationWriteAccess(principal, targetUser);

        userEvaluationRepository.save(UserEvaluation.create(userId, principal.userId(), request.content()));
    }

    /** 평가 이력 조회는 역할별 visible scope 가 다르므로 역할 분기와 대상 검증을 한 진입점에서 처리한다. */
    private void validateEvaluationReadAccess(CustomUserPrincipal principal, User targetUser) {
        UserRole principalRole = UserRole.valueOf(principal.roleCode());
        if (principalRole == UserRole.DIRECTOR) {
            validateDirectorTarget(principal, targetUser);
            return;
        }
        if (principalRole == UserRole.DEPT_HEAD) {
            validateDepartmentHeadVisibleTarget(principal, targetUser);
            return;
        }
        throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    /** 평가 등록은 자기 자신 제외 규칙을 먼저 적용한 뒤 역할별 작성 가능 범위를 검사한다. */
    private void validateEvaluationWriteAccess(CustomUserPrincipal principal, User targetUser) {
        UserRole principalRole = UserRole.valueOf(principal.roleCode());
        if (targetUser.getId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.EVALUATION_SELF_WRITE_FORBIDDEN);
        }
        if (principalRole == UserRole.DIRECTOR) {
            return;
        }
        if (principalRole == UserRole.DEPT_HEAD) {
            validateDepartmentHeadVisibleTarget(principal, targetUser);
            return;
        }
        throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    /** DIRECTOR 는 전사 조회 권한을 갖지만 본인 평가 이력은 자기 검열 방지를 위해 제외한다. */
    private void validateDirectorTarget(CustomUserPrincipal principal, User targetUser) {
        if (targetUser.getId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
        }
    }

    /** DEPT_HEAD 는 같은 부서의 실무 역할 사용자에게만 평가 조회와 등록 범위가 제한된다. */
    private void validateDepartmentHeadVisibleTarget(CustomUserPrincipal principal, User targetUser) {
        User currentUser = getUserOrThrow(principal.userId());
        if (!currentUser.getDepartmentId().equals(targetUser.getDepartmentId()) || !isDepartmentHeadVisibleRole(targetUser)) {
            throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
        }
    }

    /** DEPT_HEAD 가 조회할 수 있는 대상 역할을 TEAM_LEAD/MEMBER 로 한정한다. */
    private boolean isDepartmentHeadVisibleRole(User targetUser) {
        return targetUser.getRoleCode() == UserRole.TEAM_LEAD || targetUser.getRoleCode() == UserRole.MEMBER;
    }

    /** 사용자 존재성 검증 실패를 공통 비즈니스 예외로 변환한다. */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
