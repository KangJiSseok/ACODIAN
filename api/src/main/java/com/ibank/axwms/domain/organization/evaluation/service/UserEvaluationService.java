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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserEvaluationService {

    private static final String SORT_DIRECTION_ASC = "ASC";
    private static final String SORT_DIRECTION_DESC = "DESC";

    private final UserRepository userRepository;
    private final UserEvaluationRepository userEvaluationRepository;

    /** 사용자 평가 이력을 조회하고 접근 범위/정렬 계약을 정규화해 페이지 응답으로 반환한다. */
    public PageResponse<GetUserEvaluationsApiDto.Response.Item> getUserEvaluations(CustomUserPrincipal principal,
                                                                                   Long userId,
                                                                                   GetUserEvaluationsApiDto.Request request) {
        UserRole principalRole = UserRole.valueOf(principal.roleCode());
        User principalUser = getUserOrThrow(principal.userId());
        User targetUser = getUserOrThrow(userId);

        assertReadable(principalRole, principalUser, targetUser);

        if (principalRole == UserRole.DEPT_HEAD
                && Objects.equals(principalUser.getId(), targetUser.getId())) {
            return emptyPageResponse(request);
        }

        return GetUserEvaluationsApiDto.Response.fromPage(
                userEvaluationRepository.findUserEvaluationPage(normalizeQuery(request, targetUser.getId()))
        );
    }

    /** 사용자 평가 등록 권한을 검증한 뒤 path 대상 사용자에게 현재 사용자의 평가를 저장한다. */
    @Transactional
    public void createUserEvaluation(CustomUserPrincipal principal,
                                     Long userId,
                                     CreateUserEvaluationApiDto.Request request) {
        UserRole principalRole = UserRole.valueOf(principal.roleCode());
        User principalUser = getUserOrThrow(principal.userId());
        User targetUser = getUserOrThrow(userId);

        assertWritable(principalRole, principalUser, targetUser);

        userEvaluationRepository.save(UserEvaluation.create(
                targetUser.getId(),
                principalUser.getId(),
                request.content()
        ));
    }

    /** principal/target 사용자 모두 현재 시점 DB 기준 실존해야 하므로 없으면 USER_NOT_FOUND 를 반환한다. */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** DIRECTOR 전체 조회, DEPT_HEAD 동일 부서 조회 규칙 외 모든 read 시도는 EVALUATION_ACCESS_DENIED 로 막는다. */
    private void assertReadable(UserRole principalRole, User principalUser, User targetUser) {
        if (principalRole == UserRole.DIRECTOR) {
            return;
        }
        if (principalRole == UserRole.DEPT_HEAD
                && Objects.equals(principalUser.getDepartmentId(), targetUser.getDepartmentId())
                && targetUser.getRoleCode() != UserRole.DIRECTOR) {
            return;
        }
        throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    /** 요청 DTO 의 기본 페이지/정렬 방향을 repository 내부 query 계약으로 변환한다. */
    private UserEvaluationPageQuery normalizeQuery(GetUserEvaluationsApiDto.Request request,
                                                   Long evaluateeUserId) {
        GetUserEvaluationsApiDto.Request normalizedRequest = request == null
                ? new GetUserEvaluationsApiDto.Request(null, null, null)
                : request;
        return new UserEvaluationPageQuery(
                normalizedRequest.pageOrDefault(),
                normalizedRequest.pageSizeOrDefault(),
                evaluateeUserId,
                normalizeSortDirection(normalizedRequest.sortDirection())
        );
    }

    /** DEPT_HEAD 자기 자신 조회는 허용하되 결과는 빈 페이지로 고정한다. */
    private PageResponse<GetUserEvaluationsApiDto.Response.Item> emptyPageResponse(GetUserEvaluationsApiDto.Request request) {
        GetUserEvaluationsApiDto.Request normalizedRequest = request == null
                ? new GetUserEvaluationsApiDto.Request(null, null, null)
                : request;
        return GetUserEvaluationsApiDto.Response.fromPage(
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(normalizedRequest.pageOrDefault() - 1, normalizedRequest.pageSizeOrDefault()),
                        0
                )
        );
    }

    /** sortDirection 은 ASC/DESC 만 허용하며 null/invalid 입력은 spec 기본값인 DESC 로 정규화한다. */
    private String normalizeSortDirection(String sortDirection) {
        if (sortDirection == null) {
            return SORT_DIRECTION_DESC;
        }
        String normalized = sortDirection.toUpperCase(Locale.ROOT);
        if (SORT_DIRECTION_ASC.equals(normalized) || SORT_DIRECTION_DESC.equals(normalized)) {
            return normalized;
        }
        return SORT_DIRECTION_DESC;
    }

    /** 자기평가는 별도 오류로 거부하고, 역할/부서 write 범위 밖 요청은 접근 거부로 막는다. */
    private void assertWritable(UserRole principalRole, User principalUser, User targetUser) {
        if (Objects.equals(principalUser.getId(), targetUser.getId())) {
            throw new BusinessException(ErrorCode.EVALUATION_SELF_WRITE_FORBIDDEN);
        }
        if (principalRole == UserRole.DIRECTOR) {
            return;
        }
        if (principalRole == UserRole.DEPT_HEAD
                && Objects.equals(principalUser.getDepartmentId(), targetUser.getDepartmentId())
                && targetUser.getRoleCode() != UserRole.DIRECTOR) {
            return;
        }
        throw new BusinessException(ErrorCode.EVALUATION_ACCESS_DENIED);
    }
}
