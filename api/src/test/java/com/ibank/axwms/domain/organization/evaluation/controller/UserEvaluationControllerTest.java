package com.ibank.axwms.domain.organization.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.service.UserEvaluationService;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ExtendWith(MockitoExtension.class)
class UserEvaluationControllerTest {

    @Mock
    private UserEvaluationService userEvaluationService;

    @InjectMocks
    private UserEvaluationController userEvaluationController;

    @Test
    @DisplayName("사용자 평가 컨트롤러는 users/{userId}/evaluations 기본 경로를 사용한다")
    void 사용자_평가_컨트롤러는_users_userId_evaluations_기본_경로를_사용한다() {
        RequestMapping requestMapping = UserEvaluationController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/users/{userId}/evaluations");
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 메서드는 루트 GET 매핑을 사용한다")
    void 사용자_평가_이력_조회_메서드는_루트_GET_매핑을_사용한다() throws NoSuchMethodException {
        Method method = getUserEvaluationsMethod();
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).isEmpty();
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 메서드는 DIRECTOR 와 DEPT_HEAD role 을 허용한다")
    void 사용자_평가_이력_조회_메서드는_DIRECTOR와_DEPT_HEAD_role을_허용한다() throws NoSuchMethodException {
        Method method = getUserEvaluationsMethod();
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 userId 는 PathVariable 로 바인딩한다")
    void 사용자_평가_이력_조회_userId는_PathVariable로_바인딩한다() throws NoSuchMethodException {
        Method method = getUserEvaluationsMethod();

        assertThat(method.getParameters()[1].getAnnotation(PathVariable.class)).isNotNull();
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 요청은 ModelAttribute 로 바인딩한다")
    void 사용자_평가_이력_조회_요청은_ModelAttribute로_바인딩한다() throws NoSuchMethodException {
        Method method = getUserEvaluationsMethod();

        assertThat(method.getParameters()[2].getAnnotation(ModelAttribute.class)).isNotNull();
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 메서드는 서비스 결과 페이지를 그대로 반환한다")
    void 사용자_평가_이력_조회_메서드는_서비스_결과_페이지를_그대로_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        GetUserEvaluationsApiDto.Request request = new GetUserEvaluationsApiDto.Request(1, 20, "DESC");
        PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> responseFromService =
                GetUserEvaluationsApiDto.EvaluationSummary.fromPage(new PageImpl<>(List.of(
                        new UserEvaluationSummaryProjection(
                                501L,
                                101L,
                                "홍길동",
                                301L,
                                "김본부장",
                                "프로젝트 리딩이 안정적입니다.",
                                LocalDateTime.of(2026, 4, 20, 9, 0)
                        )
                ), PageRequest.of(0, 20), 1));
        given(userEvaluationService.getUserEvaluations(principal, 101L, request)).willReturn(responseFromService);

        PageResponse<GetUserEvaluationsApiDto.EvaluationSummary> response =
                userEvaluationController.getUserEvaluations(principal, 101L, request);

        assertThat(response).isEqualTo(responseFromService);
        assertThat(response.items()).extracting(GetUserEvaluationsApiDto.EvaluationSummary::evaluationId)
                .containsExactly(501L);
    }

    @Test
    @DisplayName("사용자 평가 이력 조회 문서 계약은 principal 을 숨김 처리한다")
    void 사용자_평가_이력_조회_문서_계약은_principal을_숨김_처리한다() throws NoSuchMethodException {
        Method method = UserEvaluationControllerDocs.class.getMethod(
                "getUserEvaluations",
                CustomUserPrincipal.class,
                Long.class,
                GetUserEvaluationsApiDto.Request.class
        );

        assertThat(method.getAnnotation(Operation.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(Parameter.class).hidden()).isTrue();
    }

    private Method getUserEvaluationsMethod() throws NoSuchMethodException {
        return UserEvaluationController.class.getMethod(
                "getUserEvaluations",
                CustomUserPrincipal.class,
                Long.class,
                GetUserEvaluationsApiDto.Request.class
        );
    }
}

