package com.ibank.axwms.global.config;

import com.ibank.axwms.global.security.JwtAuthenticationFilter;
import com.ibank.axwms.global.security.SecurityExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 로그인에서 발급한 access token 을 보호된 후속 요청에서 복원하는 보안 설정.
 * Swagger 와 로그인 엔드포인트만 익명으로 열고, 그 외 요청은 Bearer 토큰 기반으로 인증한다.
 * CSRF 는 토큰 기반 REST API 전제이므로 비활성화하고, 세션은 STATELESS 로 유지해 요청마다 JWT 로 인증을 재구성한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs",
            "/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/auth/login"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    /** Swagger 와 로그인 진입점만 익명 허용하고 나머지 API 는 인증이 필요하도록 SecurityFilterChain 을 구성한다. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
