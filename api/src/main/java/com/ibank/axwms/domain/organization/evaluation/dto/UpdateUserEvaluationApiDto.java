package com.ibank.axwms.domain.organization.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateUserEvaluationApiDto {

    /** 사용자 평가 수정 요청 DTO. */
    @Schema(description = "사용자 평가 수정 요청")
    public record Request(
            @Schema(description = "수정할 평가 내용", example = "수정된 평가 내용입니다.")
            @NotBlank(message = "content 는 필수입니다.")
            String content
    ) {
    }
}
