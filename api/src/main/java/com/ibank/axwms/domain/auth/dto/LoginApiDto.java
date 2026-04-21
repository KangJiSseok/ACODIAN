package com.ibank.axwms.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class LoginApiDto {

    private LoginApiDto() {
    }

    @Schema(description = "로그인 요청 DTO. Controller 바인딩과 AuthService 입력을 겸한다.")
    public record Request(
            @Schema(description = "로그인 이메일", example = "user@ibank.com")
            @NotBlank(message = "email 은 비어 있을 수 없습니다.")
            @Email(message = "올바른 이메일 형식이어야 합니다.")
            String email,
            @Schema(description = "평문 비밀번호", example = "Password123!")
            @NotBlank(message = "password 는 비어 있을 수 없습니다.")
            String password
    ) {
    }

    @Schema(description = "로그인 응답 DTO. 사용자 프로필은 별도 /me 엔드포인트가 SSOT 로 소유한다.")
    public record Response(
            @Schema(description = "Access Token")
            String accessToken,
            @Schema(description = "Refresh Token")
            String refreshToken
    ) {

        public static Response of(String accessToken, String refreshToken) {
            return new Response(accessToken, refreshToken);
        }
    }
}
