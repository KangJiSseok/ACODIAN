package com.ibank.axwms.domain.organization.user.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibank.axwms.testsupport.E2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

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
        mockMvc.perform(apiGet("/users/manager-candidates"))
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
        mockMvc.perform(apiGet("/users/manager-candidates"))
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
        mockMvc.perform(apiGet("/users/manager-candidates"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_ACCESS_DENIED")))
                .andExpect(jsonPath("$.error.statusCode", is(403)))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
