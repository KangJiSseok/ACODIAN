package com.ibank.axwms.domain.auth.service;

import com.ibank.axwms.domain.auth.entity.RefreshToken;
import com.ibank.axwms.domain.auth.repository.RefreshTokenRepository;
import com.ibank.axwms.domain.organization.user.entity.User;
import java.util.UUID;

import com.ibank.axwms.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleCode().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), sessionId);

        refreshTokenRepository.save(RefreshToken.issue(
                sessionId,
                user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenTtlSeconds()
        ));

        return new IssuedTokens(sessionId, accessToken, refreshToken);
    }

    public record IssuedTokens(
            String sessionId,
            String accessToken,
            String refreshToken
    ) {
    }
}
