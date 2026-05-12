package com.ibank.axwms.domain.auth.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * 한 로그인 세션에 대응하는 refresh token 레코드.
 * primary key 는 sessionId(jti) 이고, userId 는 한 사용자의 활성 세션 목록을 조회하기 위한 secondary index 로 저장한다.
 * ttlSeconds 는 Redis TTL 로 사용돼 만료된 세션은 별도 정리 작업 없이 자동 삭제된다.
 */
@RedisHash("refresh:token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private String sessionId;

    @Indexed
    private Long userId;

    private String token;

    @TimeToLive
    private Long ttlSeconds;

    /** 로그인 성공 시점에 새 세션을 구성해 저장소에 넣기 위한 RefreshToken 을 생성한다. */
    public static RefreshToken issue(String sessionId, Long userId, String token, long ttlSeconds) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.sessionId = sessionId;
        refreshToken.userId = userId;
        refreshToken.token = token;
        refreshToken.ttlSeconds = ttlSeconds;
        return refreshToken;
    }
}
