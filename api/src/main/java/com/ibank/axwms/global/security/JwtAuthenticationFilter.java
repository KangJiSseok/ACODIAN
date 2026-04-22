package com.ibank.axwms.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 HTTP 요청에서 Authorization 헤더의 Bearer access token 을 해석해 SecurityContext 를 복원하는 필터.
 * SecurityFilterChain 에서 UsernamePasswordAuthenticationFilter 앞에 배치돼,
 * 이후 권한 검사(@PreAuthorize 등)가 재조회 없이 principal 을 사용할 수 있게 해준다.
 * 요청당 한 번만 실행되도록 OncePerRequestFilter 를 상속한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CODE_CLAIM = "roleCode";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 요청마다 한 번 호출돼 Bearer 토큰이 있는지 검사하고, 있으면 인증 복원을 시도한다.
     * 헤더가 없거나 이미 인증이 채워져 있으면 인증 단계를 건너뛰고 바로 다음 필터로 위임해,
     * 익명 허용 경로(Swagger, /auth/login 등) 와 중복 인증 시나리오에서 불필요한 파싱을 피한다.
     * 토큰 검증 실패 여부와 무관하게 filterChain.doFilter 는 반드시 호출해 요청 흐름을 끊지 않는다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(authorizationHeader)
                && authorizationHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateAccessToken(request, authorizationHeader.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Bearer 에서 추출한 access token 을 검증·파싱해 SecurityContext 에 인증 객체를 주입한다.
     * tokenType 이 ACCESS 가 아니면 refresh token 오용으로 간주해 조용히 무시하고,
     * 서명/만료/형식 오류로 RuntimeException 이 발생하면 SecurityContext 를 비워 익명 상태로 떨어뜨린다.
     * 익명 상태에서 이후 authorizeHttpRequests 정책이 401/403 응답을 결정하도록 위임하는 것이 목적이다.
     */
    private void authenticateAccessToken(HttpServletRequest request, String accessToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(accessToken);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return;
            }

            String roleCode = claims.get(ROLE_CODE_CLAIM, String.class);
            CustomUserPrincipal principal = new CustomUserPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(EMAIL_CLAIM, String.class),
                    roleCode
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    accessToken,
                    buildAuthorities(roleCode)
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * roleCode claim 을 Spring Security 가 기대하는 "ROLE_" 접두 GrantedAuthority 로 변환한다.
     * roleCode 가 비어 있으면 빈 권한 목록을 돌려주어, hasRole 체크는 모두 실패하지만 인증 자체는 유지되는
     * "인증됐지만 권한 없음" 상태를 만들 수 있도록 한다.
     */
    private List<SimpleGrantedAuthority> buildAuthorities(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }
}
