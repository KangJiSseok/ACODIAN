package com.ibank.axwms.domain.worklog.entity;

import com.ibank.axwms.domain.worklog.WorklogStatus;
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
@Table(name = "tb_worklog_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorklogStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "worklog_id", nullable = false)
    private Long worklogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status_code", length = 20)
    private WorklogStatus previousStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status_code", nullable = false, length = 20)
    private WorklogStatus newStatusCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    public static WorklogStatusHistory create(
            Long worklogId,
            WorklogStatus newStatusCode,
            Long changedBy
    ) {
        WorklogStatusHistory worklogStatusHistory = new WorklogStatusHistory();
        worklogStatusHistory.worklogId = worklogId;
        worklogStatusHistory.changedBy = changedBy;
        worklogStatusHistory.newStatusCode = newStatusCode;

        return  worklogStatusHistory;
    }
}
