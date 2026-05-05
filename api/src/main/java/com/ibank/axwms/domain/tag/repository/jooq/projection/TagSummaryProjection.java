package com.ibank.axwms.domain.tag.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

import org.jooq.Record;

/** 필터 옵션용 태그 한 행 (id + name). */
public record TagSummaryProjection(
        Long tagId,
        String tagName
) {

    public static TagSummaryProjection from(Record record) {
        return new TagSummaryProjection(
                record.get(TB_META_TAG.TAG_ID),
                record.get(TB_META_TAG.TAG_NAME)
        );
    }
}
