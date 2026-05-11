package com.ibank.axwms.domain.worklog.dto;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CreateWorklogApiDto {

    private static final int CONTENT_MAX_LENGTH = 10_000;

    private CreateWorklogApiDto() {
    }

    public record Request(
            @Schema(description = "업무를 등록할 팀 ID", example = "1")
            @NotNull(message = "teamId 는 필수입니다.")
            @Positive(message = "teamId 는 양수여야 합니다.")
            Long teamId,

            @Schema(description = "업무 제목", example = "4월 결산 보고서 작성")
            @NotBlank(message = "title 은 비어 있을 수 없습니다.")
            @Size(max = 200, message = "title 은 200자를 초과할 수 없습니다.")
            String title,

            @Schema(description = "업무 요청/지시 내용", example = "재무팀 요청사항을 반영해 결산 보고서를 작성합니다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "requestContent 는 10000자를 초과할 수 없습니다.")
            String requestContent,

            @Schema(description = "실제 수행 업무 내용", example = "매출 및 비용 데이터를 집계하고 보고서 초안을 작성합니다.")
            @NotBlank(message = "workContent 는 비어 있을 수 없습니다.")
            @Size(max = CONTENT_MAX_LENGTH, message = "workContent 는 10000자를 초과할 수 없습니다.")
            String workContent,

            @Schema(description = "업무 상태 코드", example = "PENDING")
            @NotNull(message = "statusCode 는 필수입니다.")
            WorklogStatus statusCode,

            @Schema(description = "업무 중요도 코드", example = "HIGH")
            @NotNull(message = "importanceCode 는 필수입니다.")
            WorklogImportance importanceCode,

            @Schema(description = "업무 스토리 포인트", example = "3.10(3시간 10분)")
            @NotNull(message = "actualHours 는 필수입니다.")
            BigDecimal actualHours,

            @Schema(description = "업무 지시 일자", example = "2026-04-22")
            LocalDate instructionDate,

            @Schema(description = "업무 마감 일자", example = "2026-04-25")
            LocalDate dueDate,

            @Schema(description = "업무에 수동으로 연결할 태그 ID 목록", example = "[1, 2, 3]")
            List<Long> tagIds,

            List<Long> predecessorWorklogIds
    ) {
    }

    public record Response(
            Long worklogId
    ) {
        public static Response of(Long worklogId) {
            return new Response(worklogId);
        }
    }
}
