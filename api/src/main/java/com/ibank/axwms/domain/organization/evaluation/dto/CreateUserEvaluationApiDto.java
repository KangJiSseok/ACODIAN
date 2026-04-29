package com.ibank.axwms.domain.organization.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateUserEvaluationApiDto {

    @Schema(description = "사용자 평가 등록 요청 DTO")
    public record Request(
            @Schema(description = "평가 내용", example = "프로세스 정리와 리스크 대응이 우수합니다.")
            @NotBlank(message = "content 는 비어 있을 수 없습니다.")
            String content
    ) {
    }
}
