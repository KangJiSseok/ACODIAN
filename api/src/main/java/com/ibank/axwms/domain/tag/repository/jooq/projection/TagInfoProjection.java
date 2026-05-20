package com.ibank.axwms.domain.tag.repository.jooq.projection;

import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

public record TagInfoProjection(
        Long tagId,
        String tagName,
        Integer usageCount,
        String description
) {
    /**
     * jOOQ 조회 결과를 FastAPI 내부 태그 목록에 필요한 read shape 로 변환한다.
     */
    public static TagInfoProjection from(Record record) {
        return new TagInfoProjection(
                record.get(TB_META_TAG.TAG_ID),
                record.get(TB_META_TAG.TAG_NAME),
                record.get(TB_META_TAG.USAGE_COUNT),
                record.get(TB_META_TAG.DESCRIPTION)
        );
    }
}
