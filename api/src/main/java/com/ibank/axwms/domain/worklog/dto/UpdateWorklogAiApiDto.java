package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.global.enums.AiProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class UpdateWorklogAiApiDto {

    @Schema(description = "AI 업무요약 파이프라인 호출용 내부 API")
    public record Request(

            @NotNull
            @Schema(description = "AI 업무요약 내용")
            String aiSummary,

            @NotNull
            @Schema(description = "AI 업무요약 파이프라인 상태")
            AiProcessingStatus aiProcessingStatus
    ) {

    }
}
