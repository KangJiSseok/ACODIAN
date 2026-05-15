package com.ibank.axwms.domain.tag.repository.jooq;

import com.ibank.axwms.domain.tag.repository.jooq.projection.SearchTagProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagInfoProjection;
import com.ibank.axwms.domain.tag.repository.jooq.projection.TagSummaryProjection;
import com.ibank.axwms.domain.tag.repository.jooq.query.SearchTagQuery;
import org.springframework.data.domain.Page;

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
     * 메타 태그를 태그명 LIKE (대소문자 무시) 로 검색해 페이지로 반환한다.
     * query 가 null/blank 면 전체 조회와 동일하며, 정렬은 usage_count desc + tag_name asc 로 고정한다.
     */
    Page<SearchTagProjection> searchTagPage(SearchTagQuery query);
}
