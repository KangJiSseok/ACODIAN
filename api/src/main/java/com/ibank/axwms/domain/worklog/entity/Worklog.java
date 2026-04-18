package com.ibank.axwms.domain.worklog.entity;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.global.enums.AiProcessingStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_worklog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worklog_id")
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "request_content", columnDefinition = "TEXT")
    private String requestContent;

    @Column(name = "work_content", nullable = false, columnDefinition = "TEXT")
    private String workContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 20)
    private WorklogStatus statusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance_code", nullable = false, length = 20)
    private WorklogImportance importanceCode;

    @Column(name = "actual_hours", precision = 5, scale = 1)
    private BigDecimal actualHours;

    @Column(name = "instruction_date")
    private LocalDate instructionDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_summary_edited", nullable = false)
    private Boolean aiSummaryEdited;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_processing_status", nullable = false, length = 20)
    private AiProcessingStatus aiProcessingStatus;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
