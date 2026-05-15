package com.ibank.axwms.domain.tag.repository.jooq;

import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

@Repository
@RequiredArgsConstructor
public class MetaTagJooqRepositoryImpl implements MetaTagJooqRepository {

    private final DSLContext dsl;

    /**
     * 태그명 unique 제약 충돌을 DB upsert 문법으로 흡수해 병렬 AI 콜백을 멱등하게 처리한다.
     */
    @Override
    public void insertTagNamesIgnoreDuplicates(Collection<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        List<Query> insertQueries = tagNames.stream()
                .map(tagName -> dsl.insertInto(TB_META_TAG)
                        .set(TB_META_TAG.TAG_NAME, tagName)
                        .set(TB_META_TAG.USAGE_COUNT, 0)
                        .onConflict(TB_META_TAG.TAG_NAME)
                        .doNothing())
                .map(Query.class::cast)
                .toList();

        dsl.batch(insertQueries).execute();
    }

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
