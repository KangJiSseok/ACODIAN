package com.ibank.axwms.domain.worklog.entity;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_worklog_tag", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"worklog_id", "tag_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorklogTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worklog_tag_id")
    private Long id;

    @Column(name = "worklog_id", nullable = false)
    private Long worklogId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "is_ai_generated", nullable = false)
    private Boolean isAiGenerated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * AI가 선택하거나 제안한 태그를 업무일지에 연결하는 관계 엔티티를 생성한다.
     */
    public static WorklogTag createAiGenerated(Long worklogId, Long tagId) {
        WorklogTag worklogTag = new WorklogTag();
        worklogTag.worklogId = worklogId;
        worklogTag.tagId = tagId;
        worklogTag.isAiGenerated = Boolean.TRUE;
        return worklogTag;
    }
}
