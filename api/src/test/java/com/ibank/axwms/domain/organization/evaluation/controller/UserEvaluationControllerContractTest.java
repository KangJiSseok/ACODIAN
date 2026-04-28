package com.ibank.axwms.domain.organization.evaluation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;
import com.ibank.axwms.global.response.PageResponse;
import com.ibank.axwms.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class UserEvaluationControllerContractTest {

    @Test
    @DisplayName("사용자 평가 컨트롤러는 /users 기본 경로를 사용한다")
    void 사용자_평가_컨트롤러는_users_기본_경로를_사용한다() {
        RequestMapping requestMapping = UserEvaluationController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/users");
    }

    @Test
    @DisplayName("사용자 평가 조회 메서드는 canonical path 와 읽기 권한 계약을 노출한다")
    void 사용자_평가_조회_메서드는_canonical_path와_읽기_권한_계약을_노출한다() throws Exception {
        Method method = UserEvaluationController.class.getMethod(
                "getUserEvaluations",
                CustomUserPrincipal.class,
                Long.class,
                GetUserEvaluationsApiDto.Request.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/{id}/evaluations");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('DIRECTOR','DEPT_HEAD')");
    }

    @Test
    @DisplayName("사용자 평가 조회 메서드는 PageResponse<Response.Item> 반환 계약을 사용한다")
    void 사용자_평가_조회_메서드는_PageResponse_Response_Item_반환_계약을_사용한다() throws Exception {
        Method method = UserEvaluationController.class.getMethod(
                "getUserEvaluations",
                CustomUserPrincipal.class,
                Long.class,
                GetUserEvaluationsApiDto.Request.class
        );

        assertThat(method.getReturnType()).isEqualTo(PageResponse.class);
        assertThat(method.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        assertThat(returnType.getActualTypeArguments()[0].getTypeName())
                .isEqualTo(GetUserEvaluationsApiDto.Response.Item.class.getName());
    }

    @Test
    @DisplayName("UserEvaluationControllerDocs principal 파라미터는 Swagger 에서 숨긴다")
    void UserEvaluationControllerDocs_principal_파라미터는_Swagger_에서_숨긴다() throws Exception {
        Method method = UserEvaluationControllerDocs.class.getMethod(
                "getUserEvaluations",
                CustomUserPrincipal.class,
                Long.class,
                GetUserEvaluationsApiDto.Request.class
        );

        Parameter parameter = method.getParameters()[0].getAnnotation(Parameter.class);

        assertThat(parameter).isNotNull();
        assertThat(parameter.hidden()).isTrue();
    }
}
