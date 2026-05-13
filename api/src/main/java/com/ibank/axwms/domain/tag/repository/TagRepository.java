package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.entity.MetaTag;
import com.ibank.axwms.domain.tag.repository.jooq.MetaTagJooqRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<MetaTag, Long>, MetaTagJooqRepository {

    /**
     * AI가 제안한 신규 태그명이 이미 태그 풀에 존재하는지 확인하기 위해 이름 기준으로 조회한다.
     */
    List<MetaTag> findAllByTagNameIn(Collection<String> tagNames);

    /**
     * 실제 업무일지에 새로 연결된 태그 집합에 대해서만 사용 횟수 캐시를 일괄 증가시킨다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MetaTag tag set tag.usageCount = tag.usageCount + 1 where tag.id in :tagIds")
    int incrementUsageCountByIds(@Param("tagIds") Collection<Long> tagIds);

    /**
     * 업무일지 연결에서 실제 제거된 태그 집합에 대해서만 사용 횟수 캐시를 일괄 감소시킨다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MetaTag tag
               set tag.usageCount = case when tag.usageCount > 0 then tag.usageCount - 1 else 0 end
             where tag.id in :tagIds
            """)
    int decrementUsageCountByIds(@Param("tagIds") Collection<Long> tagIds);
}
