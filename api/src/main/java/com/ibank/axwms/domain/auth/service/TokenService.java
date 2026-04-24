package com.ibank.axwms.domain.auth.service;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import com.ibank.axwms.domain.auth.repository.RefreshTokenRepository;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.ibank.axwms.global.security.JwtTokenProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /** 로그인 성공 사용자에게 access/refresh token 을 발급하고 refresh token 저장소에 기록한다. */
    @Transactional
    public IssuedTokens issueLoginTokens(User user) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = issueAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), sessionId);

        saveRefreshToken(sessionId, user.getId(), refreshToken);

        return new IssuedTokens(sessionId, accessToken, refreshToken);
    }

    /**
     * refresh token 을 파싱하고 Redis 저장소와 exact match 되는지 검증한다.
     * 세부 실패 사유는 모두 AUTH_INVALID_REFRESH_TOKEN 으로 통일해 토큰 유효성 탐색 단서를 줄이지 않는다.
     */
    public ValidatedRefreshToken validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw invalidRefreshToken();
        }

        JwtTokenProvider.RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        RefreshToken savedRefreshToken = refreshTokenRepository.findById(claims.sessionId())
                .orElseThrow(this::invalidRefreshToken);

        // 방어로직
        if (!refreshToken.equals(savedRefreshToken.getToken())) {
            throw invalidRefreshToken();
        }

        return new ValidatedRefreshToken(
                claims.userId(),
                claims.sessionId(),
                shouldRotateRefreshToken(claims, Instant.now())
        );
    }

    /** 사용자 상태 재검증이 끝난 뒤 access token 을 재발급하고, 필요 시 같은 sessionId 로 refresh token 을 회전 저장한다. */
    @Transactional
    public RefreshedTokens issueRefreshTokens(User user, ValidatedRefreshToken validatedRefreshToken) {
        String accessToken = issueAccessToken(user);

        if (!validatedRefreshToken.shouldRotateRefreshToken()) {
            return new RefreshedTokens(accessToken, null);
        }

        String rotatedRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), validatedRefreshToken.sessionId());
        saveRefreshToken(validatedRefreshToken.sessionId(), user.getId(), rotatedRefreshToken);

        return new RefreshedTokens(accessToken, rotatedRefreshToken);
    }

    /** refresh JWT 자체의 iat/exp 기준으로 남은 수명을 계산해 절반 이하에서만 rotation 한다. */
    private boolean shouldRotateRefreshToken(JwtTokenProvider.RefreshTokenClaims claims, Instant now) {
        long totalLifetimeMillis = Duration.between(claims.issuedAt(), claims.expiresAt()).toMillis();
        long remainingLifetimeMillis = Duration.between(now, claims.expiresAt()).toMillis();
        return remainingLifetimeMillis <= totalLifetimeMillis / 2;
    }

    /** access token claim 조립 규칙을 한 곳에 모아 로그인/재발급 경로의 발급 로직 drift 를 막는다. */
    private String issueAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleCode().name());
    }

    /** Redis refresh 저장 형식은 sessionId/userId/token/TTL 조합으로 고정해 로그인과 rotation 양쪽에서 재사용한다. */
    private void saveRefreshToken(String sessionId, Long userId, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.issue(
                sessionId,
                userId,
                refreshToken,
                jwtTokenProvider.getRefreshTokenTtlSeconds()
        ));
    }

    /** refresh 재발급은 저장소 미존재·불일치·형식 오류를 모두 동일한 401 코드로 처리한다. */
    private BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    /**
     * refresh token 에 대응하는 현재 세션을 저장소에서 제거한다.
     * logout API 는 멱등 성공 정책을 따르므로 토큰 누락, 만료, 서명 오류, 저장소 미존재를 모두 no-op 으로 흡수한다.
     */
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!JwtTokenProvider.REFRESH_TOKEN_TYPE.equals(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class))) {
                return;
            }

            String sessionId = claims.getId();
            if (!StringUtils.hasText(sessionId)) {
                return;
            }

            refreshTokenRepository.findById(sessionId)
                    .filter(savedToken -> refreshToken.equals(savedToken.getToken()))
                    .ifPresent(refreshTokenRepository::delete);
        } catch (JwtException | IllegalArgumentException exception) {
            // 로그아웃은 멱등 성공 정책을 따르므로 잘못된/만료된 refresh token 은 조용히 무시한다.
        }
    }

    public record IssuedTokens(
            String sessionId,
            String accessToken,
            String refreshToken
    ) {
    }

    public record ValidatedRefreshToken(
            Long userId,
            String sessionId,
            boolean shouldRotateRefreshToken
    ) {
    }

    public record RefreshedTokens(
            String accessToken,
            String rotatedRefreshToken
    ) {
    }
}
