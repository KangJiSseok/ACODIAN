package com.ibank.axwms.domain.organization.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ibank.axwms.domain.organization.evaluation.dto.CreateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.domain.organization.evaluation.dto.UpdateUserEvaluationApiDto;
import com.ibank.axwms.domain.organization.evaluation.repository.jooq.projection.UserEvaluationSummaryProjection;
import com.ibank.axwms.domain.organization.evaluation.service.UserEvaluationService;
import com.ibank.axwms.global.response.EmptyResponse;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

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
    @DisplayName("사용자 평가 등록 메서드는 루트 POST 매핑과 CREATED 상태를 사용한다")
    void 사용자_평가_등록_메서드는_루트_POST_매핑과_CREATED_상태를_사용한다() throws NoSuchMethodException {
        Method method = createUserEvaluationMethod();
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).isEmpty();
        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("사용자 평가 등록 메서드는 DIRECTOR 와 DEPT_HEAD role 을 허용한다")
    void 사용자_평가_등록_메서드는_DIRECTOR와_DEPT_HEAD_role을_허용한다() throws NoSuchMethodException {
        Method method = createUserEvaluationMethod();
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("사용자 평가 수정 메서드는 ID 하위 PATCH 매핑을 사용한다")
    void 사용자_평가_수정_메서드는_ID_하위_PATCH_매핑을_사용한다() throws NoSuchMethodException {
        Method method = updateUserEvaluationMethod();
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/{id}");
    }

    @Test
    @DisplayName("사용자 평가 수정 메서드는 controller role gate 를 두지 않는다")
    void 사용자_평가_수정_메서드는_controller_role_gate를_두지_않는다() throws NoSuchMethodException {
        Method method = updateUserEvaluationMethod();

        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
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
    @DisplayName("사용자 평가 등록 요청은 RequestBody 와 Valid 로 검증한다")
    void 사용자_평가_등록_요청은_RequestBody와_Valid로_검증한다() throws NoSuchMethodException {
        Method method = createUserEvaluationMethod();

        assertThat(method.getParameters()[1].getAnnotation(PathVariable.class)).isNotNull();
        assertThat(method.getParameters()[2].getAnnotation(RequestBody.class)).isNotNull();
        assertThat(method.getParameters()[2].getAnnotation(Valid.class)).isNotNull();
    }

    @Test
    @DisplayName("사용자 평가 수정 요청은 userId 와 id PathVariable 및 RequestBody 로 바인딩한다")
    void 사용자_평가_수정_요청은_userId와_id_PathVariable_및_RequestBody로_바인딩한다() throws NoSuchMethodException {
        Method method = updateUserEvaluationMethod();

        assertThat(method.getParameters()[1].getAnnotation(PathVariable.class)).isNotNull();
        assertThat(method.getParameters()[2].getAnnotation(PathVariable.class)).isNotNull();
        assertThat(method.getParameters()[3].getAnnotation(RequestBody.class)).isNotNull();
        assertThat(method.getParameters()[3].getAnnotation(Valid.class)).isNotNull();
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
    @DisplayName("사용자 평가 등록 메서드는 서비스에 위임하고 빈 응답을 반환한다")
    void 사용자_평가_등록_메서드는_서비스에_위임하고_빈_응답을_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "director@ibank.com", "DIRECTOR");
        CreateUserEvaluationApiDto.Request request = new CreateUserEvaluationApiDto.Request("프로세스 정리가 우수합니다.");

        EmptyResponse response = userEvaluationController.createUserEvaluation(principal, 101L, request);

        assertThat(response).isEqualTo(EmptyResponse.INSTANCE);
        then(userEvaluationService).should().createUserEvaluation(principal, 101L, request);
    }

    @Test
    @DisplayName("사용자 평가 수정 메서드는 서비스에 위임하고 빈 응답을 반환한다")
    void 사용자_평가_수정_메서드는_서비스에_위임하고_빈_응답을_반환한다() {
        CustomUserPrincipal principal = new CustomUserPrincipal(301L, "evaluator@ibank.com", "DEPT_HEAD");
        UpdateUserEvaluationApiDto.Request request = new UpdateUserEvaluationApiDto.Request("수정된 평가 내용입니다.");

        EmptyResponse response = userEvaluationController.updateUserEvaluation(principal, 101L, 501L, request);

        assertThat(response).isEqualTo(EmptyResponse.INSTANCE);
        then(userEvaluationService).should().updateUserEvaluation(principal, 101L, 501L, request);
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

    @Test
    @DisplayName("사용자 평가 등록 문서 계약은 principal 을 숨김 처리한다")
    void 사용자_평가_등록_문서_계약은_principal을_숨김_처리한다() throws NoSuchMethodException {
        Method method = UserEvaluationControllerDocs.class.getMethod(
                "createUserEvaluation",
                CustomUserPrincipal.class,
                Long.class,
                CreateUserEvaluationApiDto.Request.class
        );

        assertThat(method.getAnnotation(Operation.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(Parameter.class).hidden()).isTrue();
    }

    @Test
    @DisplayName("사용자 평가 수정 문서 계약은 principal 을 숨김 처리한다")
    void 사용자_평가_수정_문서_계약은_principal을_숨김_처리한다() throws NoSuchMethodException {
        Method method = UserEvaluationControllerDocs.class.getMethod(
                "updateUserEvaluation",
                CustomUserPrincipal.class,
                Long.class,
                Long.class,
                UpdateUserEvaluationApiDto.Request.class
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

    private Method createUserEvaluationMethod() throws NoSuchMethodException {
        return UserEvaluationController.class.getMethod(
                "createUserEvaluation",
                CustomUserPrincipal.class,
                Long.class,
                CreateUserEvaluationApiDto.Request.class
        );
    }

    /** PATCH 계약 테스트가 구현 메서드 시그니처 drift 를 같은 기준으로 감지하도록 reflection 대상을 고정한다. */
    private Method updateUserEvaluationMethod() throws NoSuchMethodException {
        return UserEvaluationController.class.getMethod(
                "updateUserEvaluation",
                CustomUserPrincipal.class,
                Long.class,
                Long.class,
                UpdateUserEvaluationApiDto.Request.class
        );
    }
}

