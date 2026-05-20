package com.ibank.axwms.domain.tag.repository.jooq.projection;

import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

public record SearchTagProjection(
        Long id,
        String tagName,
        Integer usageCount,
        String description
) {
    /**
     * 태그 목록 화면에서 필요한 조회 전용 필드를 API DTO 경계로 넘길 수 있게 묶는다.
     */
    public static SearchTagProjection from(Record record) {
        return new SearchTagProjection(
                record.get(TB_META_TAG.TAG_ID),
                record.get(TB_META_TAG.TAG_NAME),
                record.get(TB_META_TAG.USAGE_COUNT),
                record.get(TB_META_TAG.DESCRIPTION)
        );
    }
}
