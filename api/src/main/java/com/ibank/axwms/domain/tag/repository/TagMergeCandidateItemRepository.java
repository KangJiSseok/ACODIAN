package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.entity.TagMergeCandidateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagMergeCandidateItemRepository extends JpaRepository<TagMergeCandidateItem, Long> {

    /**
     * 후보 목록 응답에서 그룹별 source 태그를 조립하기 위해 일괄 조회한다.
     */
    List<TagMergeCandidateItem> findByMergeCandidateIdIn(Collection<Long> mergeCandidateIds);
}
