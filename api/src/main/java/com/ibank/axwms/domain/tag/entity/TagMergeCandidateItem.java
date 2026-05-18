package com.ibank.axwms.domain.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_tag_merge_candidate_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merge_candidate_id", "source_tag_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagMergeCandidateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merge_candidate_item_id")
    private Long id;

    @Column(name = "merge_candidate_id", nullable = false)
    private Long mergeCandidateId;

    @Column(name = "source_tag_id", nullable = false)
    private Long sourceTagId;

    @Column(name = "source_tag_name", nullable = false, length = 100)
    private String sourceTagName;

    /**
     * 병합 시 사라질 source 태그 snapshot 을 후보 그룹에 묶어 저장한다.
     */
    public static TagMergeCandidateItem create(Long mergeCandidateId, Long sourceTagId, String sourceTagName) {
        TagMergeCandidateItem item = new TagMergeCandidateItem();
        item.mergeCandidateId = mergeCandidateId;
        item.sourceTagId = sourceTagId;
        item.sourceTagName = sourceTagName;
        return item;
    }
}
