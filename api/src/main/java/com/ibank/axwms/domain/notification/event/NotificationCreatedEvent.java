package com.ibank.axwms.domain.notification.event;

import java.time.LocalDateTime;

/**
 * Notification 저장 커밋 이후 실시간 전파 계층에 넘길 불변 snapshot 이다.
 */
public record NotificationCreatedEvent(
        Long notificationId,
        Long userId,
        String type,
        String title,
        String content,
        String referenceType,
        Long referenceId,
        LocalDateTime createdAt
) {
}
