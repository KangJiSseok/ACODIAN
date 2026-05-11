package com.ibank.axwms.global.config;

import com.ibank.axwms.global.logging.TraceConstants;
import com.ibank.axwms.global.security.CorsProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * refresh 쿠키 주고받기와 accessToken Authorization·X-Trace-Id 헤더 노출을 위해 CORS 정책을 구성하는 설정 클래스.
 * 허용 오리진은 CorsProperties 로만 조정하고, credentials 허용 여부·노출 헤더는 보안 정책상 여기 하드코딩한다.
 */
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    /** AuthTokenResponseWriter 의 Authorization 과 장애 신고용 traceId 를 브라우저 JS 에서 읽을 수 있도록 expose 한다. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, TraceConstants.TRACE_ID_HEADER));
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, TraceConstants.TRACE_ID_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
