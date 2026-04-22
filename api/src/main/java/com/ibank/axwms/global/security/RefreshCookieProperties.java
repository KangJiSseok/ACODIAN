package com.ibank.axwms.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * refresh token 을 HttpOnly 쿠키로 내려보낼 때 적용할 속성 묶음.
 * 환경별로 Secure/SameSite/Max-Age 가 달라져야 하므로 프로퍼티 한 곳에서만 조정할 수 있도록 묶어 둔다.
 * maxAgeSecondsOverride 가 null 이면 JWT refresh 만료 시간을 그대로 재사용한다.
 */
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public record RefreshCookieProperties(
        String name,
        String path,
        boolean secure,
        String sameSite,
        String domain,
        Long maxAgeSecondsOverride
) {
}
