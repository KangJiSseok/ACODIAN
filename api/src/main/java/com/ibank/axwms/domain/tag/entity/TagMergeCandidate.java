package com.ibank.axwms.domain.tag.entity;

import com.ibank.axwms.domain.tag.TagMergeCandidateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_tag_merge_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagMergeCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merge_candidate_id")
    private Long id;

    @Column(name = "target_tag_id", nullable = false)
    private Long targetTagId;

    @Column(name = "target_tag_name", nullable = false, length = 100)
    private String targetTagName;

    @Column(name = "result_description", length = 150)
    private String resultDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 30)
    private TagMergeCandidateStatus statusCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * AI가 추천한 병합 결과 태그 snapshot 과 적용 시 반영할 설명을 함께 저장한다.
     */
    public static TagMergeCandidate create(Long targetTagId, String targetTagName, String resultDescription) {
        TagMergeCandidate candidate = new TagMergeCandidate();
        candidate.targetTagId = targetTagId;
        candidate.targetTagName = targetTagName;
        candidate.resultDescription = resultDescription;
        candidate.statusCode = TagMergeCandidateStatus.PENDING;
        return candidate;
    }

    /**
     * 병합이 끝난 후보를 재처리할 수 없도록 상태를 고정한다.
     */
    public void markMerged() {
        this.statusCode = TagMergeCandidateStatus.APPLIED;
    }

    /**
     * 운영자가 수정한 병합 방향과 결과 설명을 적용 전 snapshot 에 반영한다.
     */
    public void updateSnapshot(Long targetTagId, String targetTagName, String resultDescription) {
        this.targetTagId = targetTagId;
        this.targetTagName = targetTagName;
        this.resultDescription = resultDescription;
    }
}
