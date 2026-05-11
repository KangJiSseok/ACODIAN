package com.ibank.axwms.domain.notification.event;

import com.ibank.axwms.domain.notification.redis.NotificationRedisStreamPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notification DB commit 이후 Redis Stream 전파만 수행하는 트랜잭션 후속 경계다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisStreamEventHandler {

    private final NotificationRedisStreamPublisher notificationRedisStreamPublisher;

    /**
     * Redis 장애가 DB 알림 생성 성공을 되돌리지 않도록 handler 경계에서 예외를 흡수한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationCreatedEvent event) {
        try {
            notificationRedisStreamPublisher.publish(event);
        } catch (RuntimeException e) {
            log.error("Redis Stream 알림 발행 실패 notificationId={} userId={}",
                    event.notificationId(), event.userId(), e);
        }
    }
}
