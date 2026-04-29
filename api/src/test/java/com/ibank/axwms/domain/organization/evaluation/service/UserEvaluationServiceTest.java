package com.ibank.axwms.domain.organization.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserEvaluationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEvaluationRepository userEvaluationRepository;

    @InjectMocks
    private UserEvaluationService userEvaluationService;

    @Test
    @DisplayName("DIRECTOR 는 타 부서 대상 사용자의 평가를 조회할 수 있다")
    void DIRECTOR_는_타_부서_대상_사용자의_평가를_조회할_수_있다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        User principalUser = createUser(1L, 10L, UserRole.DIRECTOR, "본부장");
        User targetUser = createUser(2L, 20L, UserRole.MEMBER, "사원");
        UserEvaluationPageQuery expectedQuery = new UserEvaluationPageQuery(1, 20, 2L, "DESC");
        given(userRepository.findById(1L)).willReturn(Optional.of(principalUser));
        given(userRepository.findById(2L)).willReturn(Optional.of(targetUser));
        given(userEvaluationRepository.findUserEvaluationPage(expectedQuery)).willReturn(new PageImpl<>(
                List.of(new UserEvaluationSummaryProjection(101L, 2L, "대상 사용자", 1L, "평가자", "평가 내용", LocalDateTime.of(2026, 4, 20, 9, 0))),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<GetUserEvaluationsApiDto.Response.Item> response = userEvaluationService.getUserEvaluations(
                principal,
                2L,
                new GetUserEvaluationsApiDto.Request(1, 20, null)
        );

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.evaluationId()).isEqualTo(101L);
            assertThat(item.evaluateeUserId()).isEqualTo(2L);
            assertThat(item.evaluatorUserName()).isEqualTo("평가자");
        });
    }

    @Test
    @DisplayName("DEPT_HEAD 는 같은 부서 대상 사용자의 평가를 조회할 수 있다")
    void DEPT_HEAD_는_같은_부서_대상_사용자의_평가를_조회할_수_있다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(10L, "head@ibank.com", "DEPT_HEAD");
        User principalUser = createUser(10L, 100L, UserRole.DEPT_HEAD, "부서장");
        User targetUser = createUser(11L, 100L, UserRole.MEMBER, "사원");
        ArgumentCaptor<UserEvaluationPageQuery> queryCaptor = ArgumentCaptor.forClass(UserEvaluationPageQuery.class);
        given(userRepository.findById(10L)).willReturn(Optional.of(principalUser));
        given(userRepository.findById(11L)).willReturn(Optional.of(targetUser));
        given(userEvaluationRepository.findUserEvaluationPage(queryCaptor.capture())).willReturn(Page.empty(PageRequest.of(0, 20)));

        userEvaluationService.getUserEvaluations(principal, 11L, new GetUserEvaluationsApiDto.Request(1, 20, "ASC"));

        assertThat(queryCaptor.getValue()).isEqualTo(new UserEvaluationPageQuery(1, 20, 11L, "ASC"));
    }

    @Test
    @DisplayName("DEPT_HEAD 가 같은 부서 DIRECTOR 를 조회하면 EVALUATION_ACCESS_DENIED 예외를 던진다")
    void DEPT_HEAD_가_같은_부서_DIRECTOR를_조회하면_EVALUATION_ACCESS_DENIED_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(10L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(10L)).willReturn(Optional.of(createUser(10L, 100L, UserRole.DEPT_HEAD, "부서장")));
        given(userRepository.findById(12L)).willReturn(Optional.of(createUser(12L, 100L, UserRole.DIRECTOR, "본부장")));

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 12L, new GetUserEvaluationsApiDto.Request(1, 20, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("DEPT_HEAD 가 타 부서 대상 사용자를 조회하면 EVALUATION_ACCESS_DENIED 예외를 던진다")
    void DEPT_HEAD_가_타_부서_대상_사용자를_조회하면_EVALUATION_ACCESS_DENIED_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(10L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(10L)).willReturn(Optional.of(createUser(10L, 100L, UserRole.DEPT_HEAD, "부서장")));
        given(userRepository.findById(11L)).willReturn(Optional.of(createUser(11L, 200L, UserRole.MEMBER, "사원")));

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 11L, new GetUserEvaluationsApiDto.Request(1, 20, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.EVALUATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void 대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(10L, "head@ibank.com", "DEPT_HEAD");
        given(userRepository.findById(10L)).willReturn(Optional.of(createUser(10L, 100L, UserRole.DEPT_HEAD, "부서장")));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userEvaluationService.getUserEvaluations(principal, 999L, new GetUserEvaluationsApiDto.Request(1, 20, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("DEPT_HEAD 자기 자신 조회 시 빈 페이지를 반환한다")
    void DEPT_HEAD_자기_자신_조회_시_빈_페이지를_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(10L, "head@ibank.com", "DEPT_HEAD");
        User self = createUser(10L, 100L, UserRole.DEPT_HEAD, "부서장");
        given(userRepository.findById(10L)).willReturn(Optional.of(self));

        PageResponse<GetUserEvaluationsApiDto.Response.Item> response =
                userEvaluationService.getUserEvaluations(principal, 10L, new GetUserEvaluationsApiDto.Request(2, 5, null));

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    @DisplayName("invalid/null sortDirection 은 createdAt DESC 로 정규화된다")
    void invalid_null_sortDirection은_createdAt_DESC로_정규화된다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        given(userRepository.findById(1L)).willReturn(Optional.of(createUser(1L, 10L, UserRole.DIRECTOR, "본부장")));
        given(userRepository.findById(2L)).willReturn(Optional.of(createUser(2L, 20L, UserRole.MEMBER, "사원")));
        ArgumentCaptor<UserEvaluationPageQuery> queryCaptor = ArgumentCaptor.forClass(UserEvaluationPageQuery.class);
        given(userEvaluationRepository.findUserEvaluationPage(queryCaptor.capture())).willReturn(Page.empty(PageRequest.of(0, 20)));

        userEvaluationService.getUserEvaluations(principal, 2L, new GetUserEvaluationsApiDto.Request(null, null, "sideways"));

        assertThat(queryCaptor.getValue()).isEqualTo(new UserEvaluationPageQuery(1, 20, 2L, "DESC"));
    }

    private User createUser(Long id, Long departmentId, UserRole role, String titleName) {
        User user = User.create(
                departmentId,
                "사용자",
                "user-" + id + "@ibank.com",
                "$2a$10$fake-hashed",
                role,
                EmploymentStatus.ACTIVE,
                "과장",
                titleName,
                LocalDate.of(2025, 1, 1),
                null,
                null
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
