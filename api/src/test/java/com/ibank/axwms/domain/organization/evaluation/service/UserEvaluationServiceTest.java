package com.ibank.axwms.domain.organization.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.UpdateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.entity.UserEvaluation;
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
import org.mockito.ArgumentCaptor;
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

    @Test
    @DisplayName("DIRECTOR 는 자기 자신이 아닌 사용자에게 평가를 등록한다")
    void DIRECTOR는_자기_자신이_아닌_사용자에게_평가를_등록한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        CreateUserEvaluationApiDto.Request request = new CreateUserEvaluationApiDto.Request("프로세스 정리가 우수합니다.");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));

        userEvaluationService.createUserEvaluation(principal, 101L, request);

        ArgumentCaptor<UserEvaluation> captor = ArgumentCaptor.forClass(UserEvaluation.class);
        then(userEvaluationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getEvaluateeUserId()).isEqualTo(101L);
        assertThat(captor.getValue().getEvaluatorUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getContent()).isEqualTo("프로세스 정리가 우수합니다.");
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 MEMBER 에게 평가를 등록한다")
    void DEPT_HEAD는_같은_부서_MEMBER에게_평가를_등록한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        User currentUser = createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD);
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        CreateUserEvaluationApiDto.Request request = new CreateUserEvaluationApiDto.Request("협업이 안정적입니다.");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userRepository.findById(201L)).willReturn(Optional.of(currentUser));

        userEvaluationService.createUserEvaluation(principal, 101L, request);

        ArgumentCaptor<UserEvaluation> captor = ArgumentCaptor.forClass(UserEvaluation.class);
        then(userEvaluationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getEvaluateeUserId()).isEqualTo(101L);
        assertThat(captor.getValue().getEvaluatorUserId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("자기 자신에게 평가를 등록하면 EVALUATION_SELF_WRITE_FORBIDDEN 예외를 던진다")
    void 자기_자신에게_평가를_등록하면_EVALUATION_SELF_WRITE_FORBIDDEN_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, 10L, "김본부장", UserRole.DIRECTOR)));

        assertThatThrownBy(() -> userEvaluationService.createUserEvaluation(
                principal,
                1L,
                new CreateUserEvaluationApiDto.Request("셀프 평가")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_SELF_WRITE_FORBIDDEN);

        then(userEvaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DEPT_HEAD 가 다른 부서 사용자에게 평가를 등록하면 권한 오류를 던진다")
    void DEPT_HEAD가_다른_부서_사용자에게_평가를_등록하면_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 11L, "홍길동", UserRole.MEMBER)));
        given(userRepository.findById(201L)).willReturn(Optional.of(createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> userEvaluationService.createUserEvaluation(
                principal,
                101L,
                new CreateUserEvaluationApiDto.Request("평가 내용")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);

        then(userEvaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DEPT_HEAD 가 DIRECTOR 에게 평가를 등록하면 권한 오류를 던진다")
    void DEPT_HEAD가_DIRECTOR에게_평가를_등록하면_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(201L, "dept-head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(101L)).willReturn(Optional.of(createUser(101L, 10L, "김본부장", UserRole.DIRECTOR)));
        given(userRepository.findById(201L)).willReturn(Optional.of(createUser(201L, 10L, "박부서", UserRole.DEPT_HEAD)));

        assertThatThrownBy(() -> userEvaluationService.createUserEvaluation(
                principal,
                101L,
                new CreateUserEvaluationApiDto.Request("평가 내용")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);

        then(userEvaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 등록 대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 평가_등록_대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userEvaluationService.createUserEvaluation(
                principal,
                404L,
                new CreateUserEvaluationApiDto.Request("평가 내용")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(userEvaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 작성자 본인은 평가 내용을 수정한다")
    void 평가_작성자_본인은_평가_내용을_수정한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(301L, "evaluator@ibank.com", "DEPT_HEAD");
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        UserEvaluation evaluation = createEvaluation(501L, 101L, 301L, "기존 평가");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userEvaluationRepository.findById(501L)).willReturn(Optional.of(evaluation));

        userEvaluationService.updateUserEvaluation(
                principal,
                101L,
                501L,
                new UpdateUserEvaluationApiDto.Request("수정된 평가 내용입니다.")
        );

        assertThat(evaluation.getContent()).isEqualTo("수정된 평가 내용입니다.");
    }

    @Test
    @DisplayName("평가 작성자가 아니면 평가 수정 권한 오류를 던진다")
    void 평가_작성자가_아니면_평가_수정_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(302L, "other@ibank.com", "DEPT_HEAD");
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        UserEvaluation evaluation = createEvaluation(501L, 101L, 301L, "기존 평가");
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userEvaluationRepository.findById(501L)).willReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> userEvaluationService.updateUserEvaluation(
                principal,
                101L,
                501L,
                new UpdateUserEvaluationApiDto.Request("수정 시도")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);

        assertThat(evaluation.getContent()).isEqualTo("기존 평가");
    }

    @Test
    @DisplayName("path 사용자와 평가 대상이 다르면 평가 수정 권한 오류를 던진다")
    void path_사용자와_평가_대상이_다르면_평가_수정_권한_오류를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(301L, "evaluator@ibank.com", "DEPT_HEAD");
        User target = createUser(102L, 10L, "다른대상", UserRole.MEMBER);
        UserEvaluation evaluation = createEvaluation(501L, 101L, 301L, "기존 평가");
        given(userRepository.findById(102L)).willReturn(Optional.of(target));
        given(userEvaluationRepository.findById(501L)).willReturn(Optional.of(evaluation));

        assertThatThrownBy(() -> userEvaluationService.updateUserEvaluation(
                principal,
                102L,
                501L,
                new UpdateUserEvaluationApiDto.Request("수정 시도")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("평가 수정 대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 평가_수정_대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(301L, "evaluator@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userEvaluationService.updateUserEvaluation(
                principal,
                404L,
                501L,
                new UpdateUserEvaluationApiDto.Request("수정 시도")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(userEvaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 수정 대상 평가가 없으면 EVALUATION_NOT_FOUND 예외를 던진다")
    void 평가_수정_대상_평가가_없으면_EVALUATION_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(301L, "evaluator@ibank.com", "DEPT_HEAD");
        User target = createUser(101L, 10L, "홍길동", UserRole.MEMBER);
        given(userRepository.findById(101L)).willReturn(Optional.of(target));
        given(userEvaluationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userEvaluationService.updateUserEvaluation(
                principal,
                101L,
                999L,
                new UpdateUserEvaluationApiDto.Request("수정 시도")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_NOT_FOUND);
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

    /** 수정 권한 테스트가 DB 없이 평가 소유자와 피평가자 관계만 고정할 수 있게 평가 fixture 를 만든다. */
    private UserEvaluation createEvaluation(Long id, Long evaluateeUserId, Long evaluatorUserId, String content) {
        UserEvaluation evaluation = UserEvaluation.create(evaluateeUserId, evaluatorUserId, content);
        ReflectionTestUtils.setField(evaluation, "id", id);
        return evaluation;
    }
}

