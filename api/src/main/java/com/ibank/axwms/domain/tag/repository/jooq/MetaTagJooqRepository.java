package com.ibank.axwms.domain.tag.repository.jooq;

import com.ibank.axwms.domain.tag.repository.jooq.projection.MetaTagDetailProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;

import java.util.Collection;
import java.util.List;

public interface MetaTagJooqRepository {

    /**
     * AI 태그 콜백 재시도나 병렬 처리에서 동일 태그명이 들어와도 이미 존재하면 무시하고 없는 값만 추가한다.
     */
    void insertTagNamesIgnoreDuplicates(Collection<String> tagNames);

    /**
     * 필터 옵션 등에 노출할 모든 태그를 (id, name) 형태로 조회한다.
     */
    List<TagSummaryProjection> findAllTagSummaries();

    /**
     * 태그의 사용 횟수를 포함하여 모든 태그를 (id, name, usageCount) 형태로 조회한다.
     */
    List<TagInfoProjection> findAllTagInfo();

    /**
     * 메타 태그의 모든 컬럼을 (id, name, usageCount, createdAt, updatedAt) 형태로 이름순 조회한다.
     * 업무 등록 화면 폼 옵션 등 상세 정보가 필요한 화면용.
     */
    List<MetaTagDetailProjection> findAllTagDetails();
}
