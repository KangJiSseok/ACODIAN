package com.ibank.axwms.domain.notification.repository.jooq.projection;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import org.jooq.Record;

import java.time.LocalDate;

import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

/**
 * 실제 알림 저장 전 단계에서 D-3 업무 알림 identity 를 검수할 수 있도록 대상 업무와 수신자를 함께 담는다.
 */
public record WorklogDueSoonReminderCandidateProjection(
        Long worklogId,
        Long recipientUserId,
        Long teamId,
        String worklogTitle,
        LocalDate dueDate,
        String notificationType,
        String referenceType,
        Long referenceId
) {

    /**
     * JOOQ 조회 행을 이후 저장 단계의 알림 identity 계약까지 포함한 후보 projection 으로 고정한다.
     */
    public static WorklogDueSoonReminderCandidateProjection from(Record record) {
        Long worklogId = record.get(TB_WORKLOG.WORKLOG_ID);
        return new WorklogDueSoonReminderCandidateProjection(
                worklogId,
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.DUE_DATE),
                NotificationType.WORKLOG_DUE_SOON.name(),
                NotificationReferenceType.WORKLOG.name(),
                worklogId
        );
    }
}
