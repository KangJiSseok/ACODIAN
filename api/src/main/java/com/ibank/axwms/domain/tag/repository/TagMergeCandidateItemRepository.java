package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.entity.TagMergeCandidateItem;
import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TagMergeCandidateItemRepository extends JpaRepository<TagMergeCandidateItem, Long> {

    /**
     * 태그 병합 시 source 태그 목록을 한 번에 확보한다.
     */
    List<TagMergeCandidateItem> findByMergeCandidateId(Long mergeCandidateId);

    /**
     * 후보 목록 응답에서 그룹별 source 태그를 조립하기 위해 일괄 조회한다.
     */
    List<TagMergeCandidateItem> findByMergeCandidateIdIn(Collection<Long> mergeCandidateIds);

    /**
     * 후보 수정 시 기존 source snapshot 을 DB에서 먼저 제거해 같은 태그 재선택의 unique 충돌을 막는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from TagMergeCandidateItem item where item.mergeCandidateId = :mergeCandidateId")
    void deleteByMergeCandidateId(@Param("mergeCandidateId") Long mergeCandidateId);

    /**
     * 한 후보에서 병합 완료된 source 태그는 다른 대기 후보에서도 제거해 중복 병합 제안을 정리한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from TagMergeCandidateItem item
             where item.mergeCandidateId <> :mergeCandidateId
               and item.sourceTagId in :sourceTagIds
               and item.mergeCandidateId in (
                    select candidate.id
                      from TagMergeCandidate candidate
                     where candidate.statusCode = :statusCode
               )
            """)
    void deleteSourceItemsFromOtherCandidates(@Param("mergeCandidateId") Long mergeCandidateId,
                                              @Param("sourceTagIds") Collection<Long> sourceTagIds,
                                              @Param("statusCode") TagMergeCandidateStatus statusCode);
}
