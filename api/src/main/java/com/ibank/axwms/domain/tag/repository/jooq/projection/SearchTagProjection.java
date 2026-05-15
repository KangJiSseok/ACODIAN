package com.ibank.axwms.domain.tag.repository.jooq.projection;

import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

public record SearchTagProjection(
        Long id,
        String tagName,
        Integer usageCount
) {
    public static SearchTagProjection from(Record record) {
        return new SearchTagProjection(
                record.get(TB_META_TAG.TAG_ID),
                record.get(TB_META_TAG.TAG_NAME),
                record.get(TB_META_TAG.USAGE_COUNT)
        );
    }
}
