package com.ibank.axwms.domain.organization.user.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.testsupport.E2eTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

class UserControllerSecurityE2eTest extends E2eTestSupport {

    @Test
    @DisplayName("인증 없이 현재 사용자 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_현재_사용자_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 관리자 후보 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_관리자_후보_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/admin-candidates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 부서 후보 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_부서_후보_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/department-candidates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 사용자 목록 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_목록_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 사용자 상세 조회를 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_상세_조회를_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/101"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증 없이 사용자 부분 수정을 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_사용자_부분_수정을_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiMultipartPatch("/users/101"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEAD")
    @DisplayName("TEAM_LEAD 는 관리자 후보 조회를 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void TEAM_LEAD는_관리자_후보_조회를_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/admin-candidates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("MEMBER 는 관리자 후보 조회를 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void MEMBER는_관리자_후보_조회를_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/admin-candidates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    @DisplayName("DEPT_HEAD 는 부서 후보 조회를 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void DEPT_HEAD는_부서_후보_조회를_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/users/department-candidates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEAD")
    @DisplayName("TEAM_LEAD 는 사용자 부분 수정을 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void TEAM_LEAD는_사용자_부분_수정을_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiMultipartPatch("/users/101"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("MEMBER 는 사용자 부분 수정을 호출하면 AUTH_ACCESS_DENIED 응답을 반환한다")
    void MEMBER는_사용자_부분_수정을_호출하면_AUTH_ACCESS_DENIED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiMultipartPatch("/users/101"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** 사용자 부분 수정 API 의 multipart PATCH 요청을 context-path 포함 형태로 만든다. */
    private MockMultipartHttpServletRequestBuilder apiMultipartPatch(String path) {
        return multipart("/api" + path)
                .file(new MockMultipartFile(
                        "request",
                        "",
                        MediaType.APPLICATION_JSON_VALUE,
                        "{}".getBytes(StandardCharsets.UTF_8)
                ))
                .contextPath("/api")
                .with(request -> {
                    request.setMethod("PATCH");
                    return request;
                });
    }
}
