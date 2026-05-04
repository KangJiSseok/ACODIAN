package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.Record;

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

    public static WorklogListProjection from(Record record) {
        return new WorklogListProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.WORK_CONTENT),
                record.get(TB_WORKLOG.ACTUAL_HOURS),
                record.get(TB_WORKLOG.IMPORTANCE_CODE),
                record.get(TB_WORKLOG.AI_SUMMARY),
                record.get(TB_WORKLOG.AI_PROCESSING_STATUS),
                record.get(TB_WORKLOG.AI_SUMMARY_EDITED),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_USER.USER_NAME),
                record.get(TB_WORKLOG.INSTRUCTION_DATE),
                record.get(TB_WORKLOG.DUE_DATE)
        );
    }
}
