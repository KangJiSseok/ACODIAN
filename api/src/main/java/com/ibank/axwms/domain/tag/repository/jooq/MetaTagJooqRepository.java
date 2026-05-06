package com.ibank.axwms.domain.tag.repository.jooq;

import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;

import java.util.List;

public interface MetaTagJooqRepository {

    /**
     * 필터 옵션 등에 노출할 모든 태그를 (id, name) 형태로 조회한다.
     */
    List<TagSummaryProjection> findAllTagSummaries();

    /**
     * 태그의 사용 횟수를 포함하여 모든 태그를 (id, name, usageCount) 형태로 조회한다.
     */
    List<TagInfoProjection> findAllTagInfo();
}
