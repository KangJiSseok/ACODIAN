package com.ibank.axwms.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import com.ibank.axwms.domain.auth.repository.RefreshTokenRepository;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import com.ibank.axwms.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private Claims claims;

    @Test
    @DisplayName("refresh token 이 없으면 AUTH_INVALID_REFRESH_TOKEN 을 던진다")
    void refresh_token_이_없으면_AUTH_INVALID_REFRESH_TOKEN_을_던진다() {
        assertThatThrownBy(() -> tokenService.validateRefreshToken(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh token 파싱이 실패하면 AUTH_INVALID_REFRESH_TOKEN 을 던진다")
    void refresh_token_파싱이_실패하면_AUTH_INVALID_REFRESH_TOKEN_을_던진다() {
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
                .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));

        assertThatThrownBy(() -> tokenService.validateRefreshToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("저장된 세션이 없으면 AUTH_INVALID_REFRESH_TOKEN 을 던진다")
    void 저장된_세션이_없으면_AUTH_INVALID_REFRESH_TOKEN_을_던진다() {
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(refreshClaims("session-1", 1L, 10, 10));
        given(refreshTokenRepository.findById("session-1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateRefreshToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("저장소의 refresh token 과 다르면 AUTH_INVALID_REFRESH_TOKEN 을 던진다")
    void 저장소의_refresh_token_과_다르면_AUTH_INVALID_REFRESH_TOKEN_을_던진다() {
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(refreshClaims("session-1", 1L, 10, 10));
        given(refreshTokenRepository.findById("session-1"))
                .willReturn(Optional.of(RefreshToken.issue("session-1", 1L, "other-token", 1200L)));

        assertThatThrownBy(() -> tokenService.validateRefreshToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("남은 수명이 절반 이하이면 refresh rotation 대상으로 판단한다")
    void 남은_수명이_절반_이하이면_refresh_rotation_대상으로_판단한다() {
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(refreshClaims("session-1", 1L, 10, 10));
        given(refreshTokenRepository.findById("session-1"))
                .willReturn(Optional.of(RefreshToken.issue("session-1", 1L, REFRESH_TOKEN, 1200L)));

        TokenService.ValidatedRefreshToken result = tokenService.validateRefreshToken(REFRESH_TOKEN);

        assertThat(result.shouldRotateRefreshToken()).isTrue();
        assertThat(result.sessionId()).isEqualTo("session-1");
        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("남은 수명이 절반 초과이면 기존 refresh token 을 유지한다")
    void 남은_수명이_절반_초과이면_기존_refresh_token_을_유지한다() {
        given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(refreshClaims("session-1", 1L, 10, 11));
        given(refreshTokenRepository.findById("session-1"))
                .willReturn(Optional.of(RefreshToken.issue("session-1", 1L, REFRESH_TOKEN, 1200L)));

        TokenService.ValidatedRefreshToken result = tokenService.validateRefreshToken(REFRESH_TOKEN);

        assertThat(result.shouldRotateRefreshToken()).isFalse();
    }

    @Test
    @DisplayName("rotation 이 필요하면 같은 sessionId 로 새 refresh token 을 저장한다")
    void rotation_이_필요하면_같은_sessionId_로_새_refresh_token_을_저장한다() {
        User user = activeUser();
        TokenService.ValidatedRefreshToken validatedRefreshToken =
                new TokenService.ValidatedRefreshToken(1L, "session-1", true);
        given(jwtTokenProvider.createAccessToken(1L, "user@ibank.com", "MEMBER")).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(1L, "session-1")).willReturn("rotated-refresh-token");
        given(jwtTokenProvider.getRefreshTokenTtlSeconds()).willReturn(1200L);

        TokenService.RefreshedTokens result = tokenService.issueRefreshTokens(user, validatedRefreshToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken savedToken = captor.getValue();

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rotatedRefreshToken()).isEqualTo("rotated-refresh-token");
        assertThat(savedToken.getSessionId()).isEqualTo("session-1");
        assertThat(savedToken.getToken()).isEqualTo("rotated-refresh-token");
        assertThat(savedToken.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("rotation 이 필요하지 않으면 access token 만 재발급한다")
    void rotation_이_필요하지_않으면_access_token_만_재발급한다() {
        User user = activeUser();
        TokenService.ValidatedRefreshToken validatedRefreshToken =
                new TokenService.ValidatedRefreshToken(1L, "session-1", false);
        given(jwtTokenProvider.createAccessToken(1L, "user@ibank.com", "MEMBER")).willReturn("new-access-token");

        TokenService.RefreshedTokens result = tokenService.issueRefreshTokens(user, validatedRefreshToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rotatedRefreshToken()).isNull();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_token_세션이_저장소에_있고_값이_일치하면_삭제한다() {
        RefreshToken savedToken = RefreshToken.issue("session-1", 1L, "refresh-token", 100L);
        given(jwtTokenProvider.parseClaims("refresh-token")).willReturn(claims);
        given(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class)).willReturn(JwtTokenProvider.REFRESH_TOKEN_TYPE);
        given(claims.getId()).willReturn("session-1");
        given(refreshTokenRepository.findById("session-1")).willReturn(Optional.of(savedToken));

        tokenService.revokeRefreshToken("refresh-token");

        verify(refreshTokenRepository).delete(savedToken);
    }

    @Test
    void refresh_token_이_없으면_조용히_종료한다() {
        tokenService.revokeRefreshToken(null);

        verify(jwtTokenProvider, never()).parseClaims("refresh-token");
        verify(refreshTokenRepository, never()).findById("session-1");
    }

    @Test
    void 잘못된_refresh_token_이면_예외없이_종료한다() {
        given(jwtTokenProvider.parseClaims("broken-token")).willThrow(new JwtException("invalid") {
        });

        tokenService.revokeRefreshToken("broken-token");

        verify(refreshTokenRepository, never()).findById("session-1");
    }

    @Test
    void access_token_이면_저장소를_건드리지_않는다() {
        given(jwtTokenProvider.parseClaims("access-token")).willReturn(claims);
        given(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class)).willReturn(JwtTokenProvider.ACCESS_TOKEN_TYPE);

        tokenService.revokeRefreshToken("access-token");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void sessionId_가_blank_이면_저장소를_건드리지_않는다() {
        given(jwtTokenProvider.parseClaims("refresh-token")).willReturn(claims);
        given(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class)).willReturn(JwtTokenProvider.REFRESH_TOKEN_TYPE);
        given(claims.getId()).willReturn("   ");

        tokenService.revokeRefreshToken("refresh-token");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void 저장된_token_값이_다르면_삭제하지_않는다() {
        RefreshToken savedToken = RefreshToken.issue("session-1", 1L, "other-token", 100L);
        given(jwtTokenProvider.parseClaims("refresh-token")).willReturn(claims);
        given(claims.get(JwtTokenProvider.TOKEN_TYPE_CLAIM, String.class)).willReturn(JwtTokenProvider.REFRESH_TOKEN_TYPE);
        given(claims.getId()).willReturn("session-1");
        given(refreshTokenRepository.findById("session-1")).willReturn(Optional.of(savedToken));

        tokenService.revokeRefreshToken("refresh-token");

        verify(refreshTokenRepository, never()).delete(savedToken);
    }

    private JwtTokenProvider.RefreshTokenClaims refreshClaims(String sessionId, Long userId, long issuedSecondsAgo, long expiresSecondsAfter) {
        Instant now = Instant.now();
        return new JwtTokenProvider.RefreshTokenClaims(
                userId,
                sessionId,
                now.minus(issuedSecondsAgo, ChronoUnit.SECONDS),
                now.plus(expiresSecondsAfter, ChronoUnit.SECONDS)
        );
    }

    private User activeUser() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(user.getEmail()).willReturn("user@ibank.com");
        given(user.getRoleCode()).willReturn(UserRole.MEMBER);
        return user;
    }
}
