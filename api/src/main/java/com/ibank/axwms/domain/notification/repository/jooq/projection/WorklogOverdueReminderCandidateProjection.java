package com.ibank.axwms.domain.notification.repository.jooq.projection;

import com.ibank.axwms.domain.notification.NotificationReferenceType;
import com.ibank.axwms.domain.notification.NotificationType;
import org.jooq.Record;

import java.time.LocalDate;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

/**
 * 당일/초과 마감 배치가 기존 알림을 갱신할 수 있도록 업무 identity 와 최신 조직 범위를 함께 담는다.
 */
public record WorklogOverdueReminderCandidateProjection(
        Long recipientUserId,
        Long departmentId,
        Long teamId,
        String teamName,
        String worklogTitle,
        LocalDate dueDate,
        String notificationType,
        String referenceType,
        Long referenceId
) {

    /**
     * 기준일과 마감일의 관계로 당일/초과 알림 타입을 나눠 저장 계약을 미리 고정한다.
     */
    public static WorklogOverdueReminderCandidateProjection from(Record record, LocalDate today) {
        Long referenceId = record.get(TB_WORKLOG.WORKLOG_ID);
        LocalDate dueDate = record.get(TB_WORKLOG.DUE_DATE);
        return new WorklogOverdueReminderCandidateProjection(
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_TEAM.DEPARTMENT_ID),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_WORKLOG.TITLE),
                dueDate,
                resolveNotificationType(dueDate, today).name(),
                NotificationReferenceType.WORKLOG.name(),
                referenceId
        );
    }

    /**
     * 오늘 마감과 이미 지난 마감을 서로 다른 알림 타입으로 저장해 클라이언트가 상태를 구분하게 한다.
     */
    private static NotificationType resolveNotificationType(LocalDate dueDate, LocalDate today) {
        if (dueDate.isEqual(today)) {
            return NotificationType.WORKLOG_DUE_TODAY;
        }
        return NotificationType.WORKLOG_OVERDUE;
    }
}
