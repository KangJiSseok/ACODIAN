package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;

@Repository
@RequiredArgsConstructor
public class WorklogDependencyJooqRepositoryImpl implements WorklogDependencyJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<WorklogDependencyProjection> findDirectDependencies(Long worklogId) {
        return dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.STATUS_CODE
                )
                .from(TB_WORKLOG_DEPENDENCY)
                .join(TB_WORKLOG).on(TB_WORKLOG.WORKLOG_ID.eq(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID))
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(worklogId))
                .and(TB_WORKLOG.IS_DELETED.isFalse())
                .orderBy(TB_WORKLOG_DEPENDENCY.CREATED_AT.asc(), TB_WORKLOG_DEPENDENCY.DEPENDENCY_ID.asc())
                .fetch(WorklogDependencyProjection::from);
    }

    @Override
    public Map<Long, Long> countByWorklogIds(Collection<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return Map.of();
        }
        return dsl.select(TB_WORKLOG_DEPENDENCY.WORKLOG_ID, DSL.count())
                .from(TB_WORKLOG_DEPENDENCY)
                .where(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.in(worklogIds))
                .groupBy(TB_WORKLOG_DEPENDENCY.WORKLOG_ID)
                .fetchMap(TB_WORKLOG_DEPENDENCY.WORKLOG_ID, record -> record.get(DSL.count()).longValue());
    }

    @Override
    public List<BlockedPredecessorRowProjection> findIncompletePredecessorsByWorklogIds(Collection<Long> worklogIds) {
        if (worklogIds == null || worklogIds.isEmpty()) {
            return List.of();
        }
        var my = TB_WORKLOG.as("my");
        var pred = TB_WORKLOG.as("pred");

        return dsl.select(
                        my.WORKLOG_ID,
                        my.TITLE,
                        pred.WORKLOG_ID,
                        pred.TITLE,
                        pred.STATUS_CODE
                )
                .from(my)
                .join(TB_WORKLOG_DEPENDENCY).on(TB_WORKLOG_DEPENDENCY.WORKLOG_ID.eq(my.WORKLOG_ID))
                .join(pred).on(pred.WORKLOG_ID.eq(TB_WORKLOG_DEPENDENCY.DEPENDS_ON_WORKLOG_ID))
                .where(my.WORKLOG_ID.in(worklogIds))
                .and(pred.IS_DELETED.isFalse())
                .and(pred.STATUS_CODE.ne(WorklogStatus.COMPLETED.name()))
                .orderBy(my.WORKLOG_ID.asc(), pred.WORKLOG_ID.asc())
                .fetch(record -> BlockedPredecessorRowProjection.from(record, my, pred));
    }
}
