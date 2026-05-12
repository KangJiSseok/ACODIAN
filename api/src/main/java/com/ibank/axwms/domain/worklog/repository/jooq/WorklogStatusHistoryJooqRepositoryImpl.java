package com.ibank.axwms.domain.worklog.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_STATUS_HISTORY;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogStatusHistoryProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WorklogStatusHistoryJooqRepositoryImpl implements WorklogStatusHistoryJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<WorklogStatusHistoryProjection> findStatusHistories(Long worklogId) {
        return dsl.select(
                        TB_WORKLOG_STATUS_HISTORY.HISTORY_ID,
                        TB_WORKLOG_STATUS_HISTORY.PREVIOUS_STATUS_CODE,
                        TB_WORKLOG_STATUS_HISTORY.NEW_STATUS_CODE,
                        TB_WORKLOG_STATUS_HISTORY.REASON,
                        TB_WORKLOG_STATUS_HISTORY.CHANGED_AT,
                        TB_WORKLOG_STATUS_HISTORY.CHANGED_BY,
                        TB_USER.USER_NAME
                )
                .from(TB_WORKLOG_STATUS_HISTORY)
                .join(TB_USER).on(TB_USER.USER_ID.eq(TB_WORKLOG_STATUS_HISTORY.CHANGED_BY))
                .where(TB_WORKLOG_STATUS_HISTORY.WORKLOG_ID.eq(worklogId))
                .orderBy(TB_WORKLOG_STATUS_HISTORY.CHANGED_AT.asc(), TB_WORKLOG_STATUS_HISTORY.HISTORY_ID.asc())
                .fetch(WorklogStatusHistoryProjection::from);
    }
}
