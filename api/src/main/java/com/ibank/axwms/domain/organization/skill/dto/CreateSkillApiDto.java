package com.ibank.axwms.domain.organization.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateSkillApiDto {

    /** 사용자 스킬 등록 요청 DTO. */
    @Schema(description = "사용자 스킬 등록 요청")
    public record Request(
            @Schema(description = "스킬명", example = "의사소통 능력")
            @NotBlank(message = "skillName 은 필수입니다.")
            @Size(max = 100, message = "skillName 은 100자 이하여야 합니다.")
            String skillName,
            @Schema(description = "스킬 레벨", example = "5")
            @NotNull(message = "skillLevel 은 필수입니다.")
            @Min(value = 1, message = "skillLevel 은 1이상이어야 합니다.")
            @Max(value = 5, message = "skillLevel 은 5이하여야 합니다.")
            Short skillLevel
    ) {
    }
}
