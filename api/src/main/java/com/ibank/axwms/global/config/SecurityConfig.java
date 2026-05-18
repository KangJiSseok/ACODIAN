package com.ibank.axwms.global.config;

import com.ibank.axwms.global.logging.RequestTraceFilter;
import com.ibank.axwms.global.security.JwtAuthenticationFilter;
import com.ibank.axwms.global.security.SecurityExceptionHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 로그인에서 발급한 access token 을 보호된 후속 요청에서 복원하는 보안 설정.
 * Swagger·인증 진입점(login/logout)·공통 에러 dispatch 만 익명으로 열고, 그 외 요청은 Bearer 토큰 기반으로 인증한다.
 * CSRF 는 토큰 기반 REST API 전제이므로 비활성화하고, 세션은 STATELESS 로 유지해 요청마다 JWT 로 인증을 재구성한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final RequestMatcher[] PUBLIC_PATHS = {
            PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui.html"),
            PathPatternRequestMatcher.pathPattern("/api-docs"),
            PathPatternRequestMatcher.pathPattern("/api-docs/**"),
            PathPatternRequestMatcher.pathPattern("/v3/api-docs"),
            PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
            PathPatternRequestMatcher.pathPattern("/webjars/**"),
            PathPatternRequestMatcher.pathPattern("/auth/login"),
            PathPatternRequestMatcher.pathPattern("/auth/signup"),
            PathPatternRequestMatcher.pathPattern("/auth/logout"),
            PathPatternRequestMatcher.pathPattern("/auth/refresh"),
            PathPatternRequestMatcher.pathPattern("/error"),
            // /internal/** 은 같은 도커 브릿지의 ai 컨테이너 전용 콜백 경로다.
            // 외부 노출은 nginx 가 차단하므로 인증 면제 처리한다.
            PathPatternRequestMatcher.pathPattern("/internal/**")
    };

    private final RequestTraceFilter requestTraceFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    /** Swagger·인증 진입점·에러 dispatch 만 익명 허용하고 나머지 API 는 인증이 필요하도록 SecurityFilterChain 을 구성한다. */
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
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(requestTraceFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestTraceFilter.class);
        return http.build();
    }
}
