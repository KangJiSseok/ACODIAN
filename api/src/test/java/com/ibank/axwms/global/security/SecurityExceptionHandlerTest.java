package com.ibank.axwms.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityExceptionHandlerTest {

    private SecurityExceptionHandler securityExceptionHandler;

    @BeforeEach
    void setUp() {
        securityExceptionHandler = new SecurityExceptionHandler();
    }

    @Test
    @DisplayName("인증 실패면 AUTH_UNAUTHORIZED 공통 응답을 반환한다")
    void 인증_실패면_AUTH_UNAUTHORIZED_공통_응답을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityExceptionHandler.commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"AUTH_UNAUTHORIZED\"");
        assertThat(response.getContentAsString()).contains("\"statusCode\":401");
    }

    @Test
    @DisplayName("인가 실패면 AUTH_ACCESS_DENIED 공통 응답을 반환한다")
    void 인가_실패면_AUTH_ACCESS_DENIED_공통_응답을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityExceptionHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("forbidden")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"AUTH_ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).contains("\"statusCode\":403");
    }
}
