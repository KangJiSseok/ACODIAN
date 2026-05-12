package com.ibank.axwms.domain.tag.repository.jooq.projection;

import org.jooq.Record;

import java.time.LocalDateTime;

import static com.ibank.axwms.global.jooq.Tables.TB_META_TAG;

/** 메타 태그의 모든 컬럼을 노출하는 read 전용 projection. 폼 옵션 등 상세 정보가 필요한 화면용. */
public record MetaTagDetailProjection(
        Long tagId,
        String tagName,
        Integer usageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MetaTagDetailProjection from(Record record) {
        return new MetaTagDetailProjection(
                record.get(TB_META_TAG.TAG_ID),
                record.get(TB_META_TAG.TAG_NAME),
                record.get(TB_META_TAG.USAGE_COUNT),
                record.get(TB_META_TAG.CREATED_AT),
                record.get(TB_META_TAG.UPDATED_AT)
        );
    }
}
