package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import org.jooq.Record;

import java.time.LocalDate;

/** 대시보드/목록 위젯에서 worklog 한 행을 가볍게 표현하는 공통 projection. */
public record WorklogBriefProjection(
        Long worklogId,
        String title,
        String statusCode,
        LocalDate dueDate
) {

    public static WorklogBriefProjection from(Record record) {
        return new WorklogBriefProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.DUE_DATE)
        );
    }
}
