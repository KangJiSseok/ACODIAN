package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import com.ibank.axwms.domain.tag.entity.TagMergeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagMergeCandidateRepository extends JpaRepository<TagMergeCandidate, Long> {

    /**
     * 운영자가 처리 상태별 후보를 검토할 수 있도록 최신 생성 순서로 조회한다.
     */
    List<TagMergeCandidate> findByStatusCodeOrderByCreatedAtDesc(TagMergeCandidateStatus statusCode);

    /**
     * 상태 필터가 없는 후보 목록도 동일하게 최신 생성 순서로 제공한다.
     */
    List<TagMergeCandidate> findAllByOrderByCreatedAtDesc();
}
