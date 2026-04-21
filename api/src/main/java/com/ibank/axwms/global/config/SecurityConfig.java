package com.ibank.axwms.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 임시 보안 설정.
 * 실제 JWT 필터/도메인 권한 정책이 들어오기 전까지, Swagger UI 와 공통 응답 검증용 임시 API 만 열어두고
 * 그 외는 모두 인증 필요로 둔다. CSRF 는 토큰 기반 REST API 전제이므로 비활성화하고,
 * 세션은 STATELESS 로 두어 추후 JWT 적용에 자연스럽게 이어지도록 한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs",
            "/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/api/response-test/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
