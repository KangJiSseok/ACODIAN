package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import java.time.LocalDate;

/** 업무일지 검색 결과 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record WorklogSearchProjection(
        Long worklogId,
        String title,
        String aiSummary,
        String statusCode,
        String importanceCode,
        String aiProcessingStatus,
        Integer predecessorCount,
        Long teamId,
        String teamName,
        Long authorId,
        String authorName,
        String profileImageUrl,
        LocalDate instructionDate,
        LocalDate dueDate
) {
}
