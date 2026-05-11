package com.ibank.axwms.domain.notification.controller;

import com.ibank.axwms.global.security.CustomUserPrincipal;
import com.ibank.axwms.testsupport.E2eTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerSecurityE2eTest extends E2eTestSupport {

    @Test
    @DisplayName("인증 없이 알림 SSE 구독을 호출하면 AUTH_UNAUTHORIZED 응답을 반환한다")
    void 인증_없이_알림_SSE_구독을_호출하면_AUTH_UNAUTHORIZED_응답을_반환한다() throws Exception {
        mockMvc.perform(apiGet("/notifications/stream"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.statusCode", is(401)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("인증된 사용자는 알림 SSE 구독을 text/event-stream 으로 시작한다")
    void 인증된_사용자는_알림_SSE_구독을_text_event_stream으로_시작한다() throws Exception {
        mockMvc.perform(apiGet("/notifications/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .with(principal(101L, "user@ibank.com", "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_EVENT_STREAM_VALUE)));
    }

    /** @AuthenticationPrincipal 과 role gate 를 동시에 통과할 수 있는 테스트 인증 주체를 구성한다. */
    private RequestPostProcessor principal(Long userId, String email, String role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, role);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(authenticationToken);
    }
}
