package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_STATUS_HISTORY;

import org.jooq.Record;

import java.time.LocalDateTime;

/** 업무 상태 변경 이력 한 행. */
public record WorklogStatusHistoryProjection(
        Long historyId,
        String previousStatusCode,
        String newStatusCode,
        String reason,
        LocalDateTime changedAt,
        Long changedBy,
        String changedByName
) {

    public static WorklogStatusHistoryProjection from(Record record) {
        return new WorklogStatusHistoryProjection(
                record.get(TB_WORKLOG_STATUS_HISTORY.HISTORY_ID),
                record.get(TB_WORKLOG_STATUS_HISTORY.PREVIOUS_STATUS_CODE),
                record.get(TB_WORKLOG_STATUS_HISTORY.NEW_STATUS_CODE),
                record.get(TB_WORKLOG_STATUS_HISTORY.REASON),
                record.get(TB_WORKLOG_STATUS_HISTORY.CHANGED_AT),
                record.get(TB_WORKLOG_STATUS_HISTORY.CHANGED_BY),
                record.get(TB_USER.USER_NAME)
        );
    }
}
