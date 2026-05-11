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

    /**
     * 업무 등록 요청을 서버 기본값과 함께 초기 엔티티로 만든다.
     *
     * @param authorId 작성자 사용자 ID
     * @param teamId 소속 팀 ID
     * @param title 업무 제목
     * @param requestContent 업무 요청/지시 내용
     * @param workContent 실제 수행 업무 내용
     * @param statusCode 최초 업무 상태 코드
     * @param importanceCode 중요도 코드
     * @param actualHours 업무 소요 예상 시간
     * @param instructionDate 지시 일자
     * @param dueDate 마감 일자
     * @return 저장 전 Worklog 엔티티
     */
    public static Worklog create(Long authorId,
                                 Long teamId,
                                 String title,
                                 String requestContent,
                                 String workContent,
                                 WorklogStatus statusCode,
                                 WorklogImportance importanceCode,
                                 BigDecimal actualHours,
                                 LocalDate instructionDate,
                                 LocalDate dueDate) {
        Worklog worklog = new Worklog();
        worklog.authorId = authorId;
        worklog.teamId = teamId;
        worklog.title = title;
        worklog.requestContent = requestContent;
        worklog.workContent = workContent;
        worklog.statusCode = statusCode;
        worklog.importanceCode = importanceCode;
        worklog.actualHours = actualHours;
        worklog.instructionDate = instructionDate;
        worklog.dueDate = dueDate;
        worklog.aiSummaryEdited = Boolean.FALSE;
        worklog.aiProcessingStatus = AiProcessingStatus.PENDING;
        worklog.isDeleted = Boolean.FALSE;
        return worklog;
    }

    public void changeAiSummary(String summary) {
        this.aiSummary = summary;
    }

    /**
     * 부분 수정 — null 인 인자는 변경하지 않는다.
     * aiSummary 가 새로 들어오면 aiSummaryEdited 를 true 로 표시한다 (사용자 편집 흔적).
     * teamId 는 수정 대상에서 제외 — 팀 변경은 다른 엔드포인트로 분리.
     */
    public void updatePartial(String title,
                              String requestContent,
                              String workContent,
                              WorklogImportance importanceCode,
                              BigDecimal actualHours,
                              LocalDate instructionDate,
                              LocalDate dueDate,
                              String aiSummary) {
        if (title != null) this.title = title;
        if (requestContent != null) this.requestContent = requestContent;
        if (workContent != null) this.workContent = workContent;
        if (importanceCode != null) this.importanceCode = importanceCode;
        if (actualHours != null) this.actualHours = actualHours;
        if (instructionDate != null) this.instructionDate = instructionDate;
        if (dueDate != null) this.dueDate = dueDate;
        if (aiSummary != null) {
            this.aiSummary = aiSummary;
            this.aiSummaryEdited = Boolean.TRUE;
        }
    }

    public void completeAiSummaryProcessing() {
        this.aiProcessingStatus = AiProcessingStatus.COMPLETED;
    }

    public void failAiSummaryProcessing() {
        this.aiProcessingStatus = AiProcessingStatus.FAILED;
    }
}
