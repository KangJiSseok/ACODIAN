package com.ibank.axwms.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChangePasswordApiDto {

    /** 본인 비밀번호 변경 요청 DTO. 현재 비밀번호 재검증과 새 비밀번호 정책 검증은 AuthService 가 담당한다. */
    @Schema(description = "본인 비밀번호 변경 요청 DTO")
    public record Request(
            @Schema(description = "현재 평문 비밀번호", example = "OldPassword123!")
            @NotBlank(message = "currentPassword 는 비어 있을 수 없습니다.")
            @Size(max = 100, message = "currentPassword 는 100자를 초과할 수 없습니다.")
            String currentPassword,
            @Schema(description = "새 평문 비밀번호", example = "NewPassword123!")
            @NotBlank(message = "newPassword 는 비어 있을 수 없습니다.")
            @Size(max = 100, message = "newPassword 는 100자를 초과할 수 없습니다.")
            String newPassword
    ) {
    }
}
