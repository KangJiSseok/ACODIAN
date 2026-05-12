package com.ibank.axwms.global.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 서명과 만료 시간 정책을 외부 설정으로 바인딩하는 설정 묶음.
 * 토큰 발급/파싱 인프라와 쿠키 출력 정책이 같은 refresh 만료 시간을 공유하므로, 시간 설정의 단일 출처를 제공한다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {

    /** refresh token 만료 시간을 쿠키 Max-Age/Redis TTL 에서 재사용할 수 있도록 초 단위로 변환한다. */
    public long refreshTokenExpirationSeconds() {
        return Duration.ofMillis(refreshTokenExpiration).toSeconds();
    }
}
