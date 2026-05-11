package com.ibank.axwms.domain.file.dto;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class UpdateFileAiApiDto {

    @Schema(description = "AI 파일 요약 파이프라인 콜백용 내부 API")
    public record Request(

            @NotNull
            @Schema(description = "AI 파일 요약 내용. 실패 시에도 빈 문자열로 전송된다.")
            String aiSummary,

            @NotNull
            @Schema(description = "AI 파일 요약 파이프라인 상태")
            AiProcessingStatus aiProcessingStatus
    ) {

    }
}
