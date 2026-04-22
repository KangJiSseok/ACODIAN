package com.ibank.axwms.global.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * refresh 쿠키와 Authorization 헤더를 크로스 오리진에서 주고받으려면 CORS 설정이 필수이므로,
 * 허용 오리진을 환경별 프로퍼티로 분리해 두기 위한 설정 묶음.
 */
@ConfigurationProperties(prefix = "auth.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedOriginPatterns
) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
    }
}
