package com.ibank.axwms.domain.file.dto;

import com.ibank.axwms.domain.file.repository.jooq.projection.FileWorklogProjection;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "파일 목록 항목에 함께 노출되는 업무 요약")
public record FileWorklogItem(
        @Schema(description = "업무 ID", example = "501")
        Long worklogId,
        @Schema(description = "소속 팀 ID", example = "21")
        Long teamId,
        @Schema(description = "소속 팀명", example = "MCP Project")
        String teamName,
        @Schema(description = "작성자 사용자 ID", example = "101")
        Long authorId,
        @Schema(description = "작성자 사용자명", example = "최수빈")
        String authorName,
        @Schema(description = "업무 제목", example = "AX-WMS 디자인 시스템 정리")
        String title,
        @Schema(description = "업무 요청/지시 내용")
        String requestContent,
        @Schema(description = "실제 수행 업무 내용")
        String workContent,
        @Schema(description = "AI 업무일지 요약 내용")
        String aiSummary,
        @Schema(description = "AI 파이프라인 상태", example = "COMPLETED")
        AiProcessingStatus aiProcessingStatus,
        @Schema(description = "업무 마감 일자")
        LocalDate dueDate,
        @Schema(description = "실제 소요 시간(시간.분)", example = "7.5")
        BigDecimal actualHours,
        @Schema(description = "선행 업무 개수", example = "1")
        Integer dependencyCount
) {

    public static FileWorklogItem from(FileWorklogProjection p) {
        return new FileWorklogItem(
                p.worklogId(),
                p.teamId(),
                p.teamName(),
                p.authorId(),
                p.authorName(),
                p.title(),
                p.requestContent(),
                p.workContent(),
                p.aiSummary(),
                p.aiProcessingStatus(),
                p.dueDate(),
                p.actualHours(),
                p.dependencyCount()
        );
    }
}
