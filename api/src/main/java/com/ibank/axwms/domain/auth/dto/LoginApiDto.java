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

    /**
     * AuthService → Controller 경계에서 발급된 토큰 쌍을 전달하기 위한 내부 record.
     * Controller 는 이 값을 AuthTokenResponseWriter 로 넘겨 헤더/쿠키에 기록하고 바디에는 포함하지 않는다.
     */
    public record Result(
            String accessToken,
            String refreshToken
    ) {
    }
}
