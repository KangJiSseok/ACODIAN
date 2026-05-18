package com.ibank.axwms.domain.tag.repository;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import com.ibank.axwms.domain.tag.entity.TagMergeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 병합 적용 요청이 동시에 들어와도 PENDING 상태를 먼저 선점한 트랜잭션만 후속 병합을 수행한다.
     */
    default int markAppliedIfPending(Long candidateId) {
        return updateStatusIfPending(
                candidateId,
                TagMergeCandidateStatus.APPLIED,
                TagMergeCandidateStatus.PENDING
        );
    }

    /**
     * 상태 조건부 update 로 DB row lock 을 얻어 동시에 같은 후보가 처리되는 것을 막는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TagMergeCandidate candidate
               set candidate.statusCode = :appliedStatus
             where candidate.id = :candidateId
               and candidate.statusCode = :pendingStatus
            """)
    int updateStatusIfPending(@Param("candidateId") Long candidateId,
                              @Param("appliedStatus") TagMergeCandidateStatus appliedStatus,
                              @Param("pendingStatus") TagMergeCandidateStatus pendingStatus);
}
