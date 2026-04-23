package com.ibank.axwms.domain.auth.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RefreshAccessTokenApiDto {


    /**
     * refresh 재발급 결과를 Controller 경계로 전달한다.
     * rotatedRefreshToken 이 null 이면 기존 refresh cookie 를 유지하고 access token 헤더만 갱신한다.
     */
    public record Result(
            String accessToken,
            String rotatedRefreshToken
    ) {
    }
}
