package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import org.jooq.Record;

/** 업무 상세에 노출할 선행 업무 한 행 (depth 1). */
public record WorklogDependencyProjection(
        Long worklogId,
        String title,
        String statusCode
) {

    public static WorklogDependencyProjection from(Record record) {
        return new WorklogDependencyProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.STATUS_CODE)
        );
    }
}
