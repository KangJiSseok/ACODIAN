package com.ibank.axwms.domain.notification.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_NOTIFICATION;

import java.time.LocalDateTime;
import org.jooq.Record;

/** 내 알림 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record NotificationSearchProjection(
        Long notificationId,
        String notificationType,    // TODO enum 추가되면 반영될 예정
        String title,
        String content,
        String referenceType,   // TODO enum 추가되면 반영될 예정
        Long referenceId,
        Long departmentId,
        Long teamId,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    /** JOOQ 조회 결과 record 를 알림 목록 projection 으로 조립한다. */
    public static NotificationSearchProjection from(Record record) {
        return new NotificationSearchProjection(
                record.get(TB_NOTIFICATION.NOTIFICATION_ID),
                record.get(TB_NOTIFICATION.NOTIFICATION_TYPE),
                record.get(TB_NOTIFICATION.TITLE),
                record.get(TB_NOTIFICATION.CONTENT),
                record.get(TB_NOTIFICATION.REFERENCE_TYPE),
                record.get(TB_NOTIFICATION.REFERENCE_ID),
                record.get(TB_NOTIFICATION.DEPARTMENT_ID),
                record.get(TB_NOTIFICATION.TEAM_ID),
                record.get(TB_NOTIFICATION.IS_READ),
                record.get(TB_NOTIFICATION.READ_AT),
                record.get(TB_NOTIFICATION.CREATED_AT)
        );
    }
}
