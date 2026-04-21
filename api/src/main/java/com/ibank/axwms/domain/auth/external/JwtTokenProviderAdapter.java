package com.ibank.axwms.domain.auth.external;

import com.ibank.axwms.domain.organization.user.entity.User;
import com.ibank.axwms.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * auth 도메인이 global.security 의 JwtTokenProvider 를 직접 의존하지 않도록 감싸는 adapter.
 * 도메인 Entity(User) 를 JwtTokenProvider 가 기대하는 원시 claim 값으로 풀어 전달하는 변환 지점이다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter {

    private final JwtTokenProvider jwtTokenProvider;

    /** 로그인한 User 의 식별자/이메일/권한 코드를 claim 으로 조립한 access token 을 발급한다. */
    public String issueAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRoleCode().name());
    }

    /** 다중 세션 저장 모델에서 세션 키로 쓰일 sessionId 를 jti 로 담아 refresh token 을 발급한다. */
    public String issueRefreshToken(Long userId, String sessionId) {
        return jwtTokenProvider.createRefreshToken(userId, sessionId);
    }

    /** Redis 에 refresh token 을 저장할 때 사용할 TTL(초) 을 노출한다. */
    public long getRefreshTokenTtlSeconds() {
        return jwtTokenProvider.getRefreshTokenTtlSeconds();
    }
}
