package com.ibank.axwms.domain.notification.dto;

import com.ibank.axwms.domain.notification.redis.NotificationStreamMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 SSE 구독 API 가 실시간 이벤트 data 로 내려보내는 payload 계약을 모은다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscribeNotificationStreamApiDto {

    /**
     * SSE 연결 사용자는 자기 알림만 받으므로 내부 라우팅용 userId 는 응답 payload 에 노출하지 않는다.
     */
    public record Response(
            Long notificationId,
            String type,
            String title,
            String content,
            String referenceType,
            Long referenceId,
            LocalDateTime createdAt
    ) {

        /** Redis Stream 내부 메시지를 클라이언트가 소비하는 알림 이벤트 형태로 축약한다. */
        public static Response from(NotificationStreamMessage message) {
            return new Response(
                    message.notificationId(),
                    message.type(),
                    message.title(),
                    message.content(),
                    message.referenceType(),
                    message.referenceId(),
                    message.createdAt()
            );
        }
    }
}
