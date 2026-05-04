package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import org.jooq.Record;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 업무 상세 본문 한 행을 담는 읽기 전용 projection. */
public record WorklogDetailProjection(
        Long worklogId,
        Long teamId,
        String teamName,
        Long authorId,
        String authorName,
        String title,
        String requestContent,
        String workContent,
        String statusCode,
        BigDecimal actualHours,
        LocalDate instructionDate,
        LocalDate dueDate
) {

    public static WorklogDetailProjection from(Record record) {
        return new WorklogDetailProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_USER.USER_NAME),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.REQUEST_CONTENT),
                record.get(TB_WORKLOG.WORK_CONTENT),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.ACTUAL_HOURS),
                record.get(TB_WORKLOG.INSTRUCTION_DATE),
                record.get(TB_WORKLOG.DUE_DATE)
        );
    }
}
