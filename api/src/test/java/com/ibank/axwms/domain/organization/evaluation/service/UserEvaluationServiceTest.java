package com.ibank.axwms.domain.organization.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.repository.UserEvaluationRepository;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.query.UserEvaluationPageQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.domain.organization.user.repository.UserRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserEvaluationServiceTest {

    @Mock
    private UserEvaluationRepository userEvaluationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserEvaluationService userEvaluationService;

    @Test
    @DisplayName("DIRECTOR 는 자기 자신이 아닌 사용자의 평가 이력을 조회한다")
    void DIRECTOR는_자기_자신이_아닌_사용자의_평가_이력을_조회한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        GetUserEvaluationsApiDto.Request request = new GetUserEvaluationsApiDto.Request(1, 20, "ASC");
        UserEvaluationPageQuery expectedQuery = new UserEvaluationPageQuery(1, 20, 101L, "ASC");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userEvaluationRepository.findEvaluationPage(expectedQuery)).willReturn(new PageImpl<>(List.of(
                new UserEvaluationSummaryProjection(
                        501L,
                        101L,
                        "홍길동",
                        1L,
                        "김본부장",
                        "프로젝트 리딩이 안정적입니다.",
                        LocalDateTime.of(2026, 4, 20, 9, 0)
                )
        ), PageRequest.of(0, 20), 1));

        PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> result =
                userEvaluationService.getUserEvaluations(principal, 101L, request);

        assertThat(result.items())
                .extracting(GetUserEvaluationsApiDto.EvaluationSummary::evaluationId,
                        GetUserEvaluationsApiDto.EvaluationSummary::evaluateeUserName,
                        GetUserEvaluationsApiDto.EvaluationSummary::evaluatorUserName)
                .containsExactly(Tuple.tuple(501L, "홍길동", "김본부장"));
    }

    @Test
    @DisplayName("DIRECTOR 가 자기 자신의 평가 이력을 조회하면 권한 오류를 던진다")
    void DIRECTOR가_자기_자신의_평가_이력을_조회하면_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, 10L, "김본부장", UserRole.DIRECTOR)));

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 1L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 TEAM_LEAD 평가 이력을 조회한다")
    void DEPT_HEAD는_같은_부서_TEAM_LEAD_평가_이력을_조회한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User currentUser = createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD);
        User target = createUser(101L, 10L, "이팀장", UserRole.TEAM_LEAD);
        UserEvaluationPageQuery expectedQuery = new UserEvaluationPageQuery(1, 20, 101L, "DESC");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userRepository.findById(201L)).willReturn(Optional.of(currentUser));
        given(userEvaluationRepository.findEvaluationPage(expectedQuery)).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> result =
                userEvaluationService.getUserEvaluations(principal, 101L, null);

        assertThat(result.items()).isEmpty();
        then(userEvaluationRepository).should().findEvaluationPage(expectedQuery);
    }

    @Test
    @DisplayName("DEPT_HEAD 가 다른 부서 사용자 평가 이력을 조회하면 권한 오류를 던진다")
    void DEPT_HEAD가_다른_부서_사용자_평가_이력을_조회하면_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 11L, "홍길동", UserRole.MEMBER)));
        given(userRepository.findById(201L)).willReturn(Optional.of(createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 101L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 가 DEPT_HEAD 사용자 평가 이력을 조회하면 권한 오류를 던진다")
    void DEPT_HEAD가_DEPT_HEAD_사용자_평가_이력을_조회하면_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, "다른부서장", UserRole.DEPT_HEAD)));
        given(userRepository.findById(201L)).willReturn(Optional.of(createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 101L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 404L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User createUser(Long id, Long departmentId, String userName, UserRole roleCode) {
        User user = User.create(
                departmentId,
                userName,
                userName + "@ibank.com",
                "$2a$10$fake-hashed",
                roleCode,
                EmploymentStatus.ACTIVE,
                "과장",
                roleCode.name(),
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

