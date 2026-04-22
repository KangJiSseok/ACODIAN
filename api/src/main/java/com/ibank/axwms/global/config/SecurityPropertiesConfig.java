package com.ibank.axwms.global.config;

import com.ibank.axwms.global.security.CorsProperties;
import com.ibank.axwms.global.security.JwtProperties;
import com.ibank.axwms.global.security.RefreshCookieProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 브라우저 기반 인증/보안 인프라가 공통으로 쓰는 @ConfigurationProperties 빈 등록 전용 설정 클래스.
 * CORS 정책 조립 책임과 프로퍼티 빈 등록 책임을 분리해 설정 클래스 이름과 실제 역할을 맞춘다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class, RefreshCookieProperties.class})
public class SecurityPropertiesConfig {
}
