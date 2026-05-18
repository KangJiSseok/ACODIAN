package com.ibank.axwms.domain.worklog.repository.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.val;
import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_TAG;

@Repository
@RequiredArgsConstructor
public class WorklogTagJooqRepositoryImpl implements WorklogTagJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<String> findTagNames(Long worklogId) {
        return dsl.select(TB_META_TAG.TAG_NAME)
                .from(TB_WORKLOG_TAG)
                .join(TB_META_TAG).on(TB_META_TAG.TAG_ID.eq(TB_WORKLOG_TAG.TAG_ID))
                .where(TB_WORKLOG_TAG.WORKLOG_ID.eq(worklogId))
                .orderBy(TB_WORKLOG_TAG.CREATED_AT.asc())
                .fetch(record -> record.get(TB_META_TAG.TAG_NAME));
    }

    @Override
    public void replaceSourceTagsWithTarget(Long targetTagId, List<Long> sourceTagIds) {
        if (sourceTagIds == null || sourceTagIds.isEmpty()) {
            return;
        }

        var sourceWorklogTag = TB_WORKLOG_TAG.as("source_worklog_tag");
        dsl.insertInto(
                        TB_WORKLOG_TAG,
                        TB_WORKLOG_TAG.WORKLOG_ID,
                        TB_WORKLOG_TAG.TAG_ID
                )
                .select(dsl.select(
                                sourceWorklogTag.WORKLOG_ID,
                                val(targetTagId)
                        )
                        .from(sourceWorklogTag)
                        .where(sourceWorklogTag.TAG_ID.in(sourceTagIds))
                        .groupBy(sourceWorklogTag.WORKLOG_ID))
                .onConflict(TB_WORKLOG_TAG.WORKLOG_ID, TB_WORKLOG_TAG.TAG_ID)
                .doNothing()
                .execute();

        dsl.deleteFrom(TB_WORKLOG_TAG)
                .where(TB_WORKLOG_TAG.TAG_ID.in(sourceTagIds))
                .execute();
    }

    @Override
    public int countDistinctWorklogsByTagId(Long tagId) {
        return dsl.select(countDistinct(TB_WORKLOG_TAG.WORKLOG_ID))
                .from(TB_WORKLOG_TAG)
                .where(TB_WORKLOG_TAG.TAG_ID.eq(tagId))
                .fetchSingle(0, Integer.class);
    }
}
