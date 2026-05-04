package com.ibank.axwms.domain.worklog.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_DEPENDENCY;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
}
