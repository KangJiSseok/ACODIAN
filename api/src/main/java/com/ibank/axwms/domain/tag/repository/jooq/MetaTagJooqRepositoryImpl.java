package com.ibank.axwms.domain.tag.repository.jooq;

import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

@Repository
@RequiredArgsConstructor
public class MetaTagJooqRepositoryImpl implements MetaTagJooqRepository {

    private final DSLContext dsl;

    @Override
    public List<TagSummaryProjection> findAllTagSummaries() {
        return dsl.select(TB_META_TAG.TAG_ID, TB_META_TAG.TAG_NAME)
                .from(TB_META_TAG)
                .orderBy(TB_META_TAG.TAG_NAME.asc(), TB_META_TAG.TAG_ID.asc())
                .fetch(TagSummaryProjection::from);
    }

    @Override
    public List<TagInfoProjection> findAllTagInfo() {
        return dsl.select(TB_META_TAG.TAG_ID, TB_META_TAG.TAG_NAME, TB_META_TAG.USAGE_COUNT)
                .from(TB_META_TAG)
                .orderBy(TB_META_TAG.USAGE_COUNT.asc(), TB_META_TAG.TAG_NAME.asc(), TB_META_TAG.TAG_ID.asc())
                .fetch(TagInfoProjection::from);
    }
}
