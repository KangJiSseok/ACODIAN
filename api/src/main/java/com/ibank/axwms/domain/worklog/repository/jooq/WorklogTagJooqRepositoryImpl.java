package com.ibank.axwms.domain.worklog.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG_TAG;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
