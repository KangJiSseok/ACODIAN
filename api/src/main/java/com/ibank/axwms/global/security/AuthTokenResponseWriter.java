package com.ibank.axwms.global.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 로그인/재발급 응답에 accessToken 을 Authorization 헤더로, refreshToken 을 HttpOnly 쿠키로 기록하는 단일 창구.
 * Controller 가 직접 헤더/쿠키 속성을 조립하면 로그인·재발급·로그아웃 사이에 속성 drift 가 생기므로,
 * 쿠키 속성 정책을 이 컴포넌트 한 곳에 묶어 재사용한다.
 */
@Component
public class AuthTokenResponseWriter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RefreshCookieProperties refreshCookieProperties;
    private final JwtProperties jwtProperties;

    public AuthTokenResponseWriter(
            RefreshCookieProperties refreshCookieProperties,
            JwtProperties jwtProperties
    ) {
        this.refreshCookieProperties = refreshCookieProperties;
        this.jwtProperties = jwtProperties;
    }

    /** accessToken 을 Authorization: Bearer ... 형식으로 응답 헤더에 기록한다. */
    public void writeAccessTokenHeader(HttpServletResponse response, String accessToken) {
        response.setHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken);
    }

    /** refreshToken 을 프로퍼티에 정의된 HttpOnly 쿠키 속성으로 Set-Cookie 헤더에 기록한다. */
    public void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = buildCookie(refreshToken, resolveMaxAgeSeconds());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 로그아웃·회전 실패 시 브라우저가 쿠키를 즉시 삭제하도록 Max-Age=0 의 동일 이름 쿠키를 내려준다. */
    public void writeExpiredRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = buildCookie("", 0L);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 공통 쿠키 속성(HttpOnly, Secure, SameSite, Path, Domain) 을 한 번에 조립한다. */
    private ResponseCookie buildCookie(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieProperties.name(), value)
                .httpOnly(true)
                .secure(refreshCookieProperties.secure())
                .sameSite(refreshCookieProperties.sameSite())
                .path(refreshCookieProperties.path())
                .maxAge(Duration.ofSeconds(maxAgeSeconds));
        if (StringUtils.hasText(refreshCookieProperties.domain())) {
            builder.domain(refreshCookieProperties.domain());
        }
        return builder.build();
    }

    /** override 프로퍼티가 지정되어 있으면 그 값을 쓰고, 아니면 JWT 설정의 refresh 만료 시간과 동기화한다. */
    private long resolveMaxAgeSeconds() {
        Long override = refreshCookieProperties.maxAgeSecondsOverride();
        return override != null ? override : jwtProperties.refreshTokenExpirationSeconds();
    }
}
