package com.ibank.axwms.domain.organization.evaluation.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.testsupport.E2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

class UserEvaluationControllerSecurityE2eTest extends E2eTestSupport {

    @Test
    @DisplayName("인증 없이 사용자 평가 이력 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_평가_이력_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/101/evaluations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("MEMBER 는 사용자 평가 이력 조회를 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void MEMBER는_사용자_평가_이력_조회를_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/101/evaluations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 사용자 평가 등록을 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_평가_등록을_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/users/101/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"평가 내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEAD")
    @DisplayName("TEAM_LEAD 는 사용자 평가 등록을 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void TEAM_LEAD는_사용자_평가_등록을_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/users/101/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"평가 내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("MEMBER 는 사용자 평가 등록을 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void MEMBER는_사용자_평가_등록을_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPost("/users/101/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"평가 내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 사용자 평가 수정을 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_평가_수정을_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPatch("/users/101/evaluations/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 평가\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("사용자 평가 수정 content 가 비어 있으면 validation error 응답을 반환한다")
    void 사용자_평가_수정_content가_비어_있으면_validation_error_응답을_반환한다() throws Exception {
        mockMvc.perform(apiPatch("/users/101/evaluations/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("COMMON_VALIDATION_ERROR")))
                .andExpect(jsonPath("$.error.statusCode", is(400)))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
