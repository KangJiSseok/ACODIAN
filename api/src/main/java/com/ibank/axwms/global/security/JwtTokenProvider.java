package com.ibank.axwms.global.security;

import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * access/refresh JWT 의 서명 발급과 claim 파싱을 모두 담당하는 보안 인프라 컴포넌트.
 * 서명 키와 만료 시간은 설정값에서 한 번만 로드해 재사용하고, access/refresh 의 구조적 차이는 claim 조합으로만 구분한다.
 */
@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CODE_CLAIM = "roleCode";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMillis = jwtProperties.accessTokenExpiration();
        this.refreshTokenExpirationMillis = jwtProperties.refreshTokenExpiration();
    }

    /** 로그인 성공 사용자에게 access token 을 발급한다. */
    public String createAccessToken(Long userId, String email, String roleCode) {
        return buildToken(
                String.valueOf(userId),
                accessTokenExpirationMillis,
                builder -> builder
                        .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                        .claim(EMAIL_CLAIM, email)
                        .claim(ROLE_CODE_CLAIM, roleCode)
        );
    }

    /** refresh token 저장과 추적에 사용할 sessionId(jti) 기준 토큰을 발급한다. */
    public String createRefreshToken(Long userId, String sessionId) {
        return buildToken(
                String.valueOf(userId),
                refreshTokenExpirationMillis,
                builder -> builder
                        .id(sessionId)
                        .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
        );
    }

    /** 발급된 JWT 의 claim 을 검증 후 읽는다. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * refresh token 을 검증하고 재발급 흐름에 필요한 최소 claim 만 구조화해 반환한다.
     * subject(userId), jti(sessionId), issuedAt, expiration 이 하나라도 비정상이면 저장소 조회 전에 즉시 거부한다.
     */
    public RefreshTokenClaims parseRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            if (!REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                throw invalidRefreshToken();
            }

            String sessionId = claims.getId();
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            if (!StringUtils.hasText(sessionId) || issuedAt == null || expiration == null) {
                throw invalidRefreshToken();
            }

            return new RefreshTokenClaims(
                    parseUserId(claims.getSubject()),
                    sessionId,
                    issuedAt.toInstant(),
                    expiration.toInstant()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidRefreshToken();
        }
    }

    /** refresh token TTL 을 Redis 저장소의 만료 시간(초) 단위로 변환해 노출한다. */
    public long getRefreshTokenTtlSeconds() {
        return Duration.ofMillis(refreshTokenExpirationMillis).toSeconds();
    }

    /** refresh token 의 subject 는 userId 숫자 문자열이어야 하므로 파싱 실패도 즉시 무효 처리한다. */
    private Long parseUserId(String subject) {
        if (!StringUtils.hasText(subject)) {
            throw invalidRefreshToken();
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw invalidRefreshToken();
        }
    }

    /** refresh 재발급 흐름은 세부 실패 원인을 노출하지 않고 단일 401 코드로 묶는다. */
    private BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    /**
     * subject/발급·만료 시각 같은 공통 claim 을 채운 뒤, customizer 로 토큰 종류별 추가 claim 을 조립해 서명한다.
     * access/refresh 의 공통 골격을 한 곳에 묶어 이슈 시각 계산과 서명 키 사용처를 단일화하기 위한 helper.
     */
    private String buildToken(String subject, long expirationMillis, TokenBuilderCustomizer customizer) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)));
        customizer.customize(builder);
        return builder.signWith(signingKey).compact();
    }

    /** access/refresh 별로 다른 claim 조립 로직을 buildToken 에 람다로 주입하기 위한 내부 콜백. */
    @FunctionalInterface
    private interface TokenBuilderCustomizer {
        void customize(JwtBuilder builder);
    }

    public record RefreshTokenClaims(
            Long userId,
            String sessionId,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }
}
