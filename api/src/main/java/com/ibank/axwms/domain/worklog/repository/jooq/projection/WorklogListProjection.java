package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 업무 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record WorklogListProjection(
        Long worklogId,
        String title,
        String statusCode,
        String workContent,
        BigDecimal actualHours,
        String importanceCode,
        String aiSummary,
        String aiProcessingStatus,
        Boolean aiSummaryEdited,
        Long teamId,
        String teamName,
        Long authorId,
        String authorName,
        LocalDate instructionDate,
        LocalDate dueDate
) {
}
