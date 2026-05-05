package com.ibank.axwms.domain.organization.skill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateSkillApiDto {

    /** 사용자 스킬 부분 수정 요청 DTO. */
    @Schema(description = "사용자 스킬 부분 수정 요청")
    public record Request(
            @Schema(description = "스킬명", example = "WMS")
            @Size(max = 100, message = "skillName 은 100자 이하여야 합니다.")
            String skillName,
            @Schema(description = "스킬 레벨", example = "5")
            @Min(value = 1, message = "skillLevel 은 1 이상이어야 합니다.")
            @Max(value = 5, message = "skillLevel 은 5 이하여야 합니다.")
            Short skillLevel
    ) {
    }
}
