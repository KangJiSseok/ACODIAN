package com.ibank.axwms.domain.notification.scheduler;

import com.ibank.axwms.domain.notification.service.NotificationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 읽음 처리 후 보관 기간이 지난 알림만 정리해 사용자 확인 전 알림은 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReadCleanupScheduler {

    private final NotificationService notificationService;

    /**
     * 스케줄러 실행 시각을 기준으로 만료선을 계산하게 해 배치 지연이 있어도 읽음 후 3일 보관 계약을 지킨다.
     */
    @Scheduled(
            cron = "${notification.read-cleanup.cron}"
    )
    public void deleteExpiredReadNotifications() {
        LocalDateTime now = LocalDateTime.now();
        try {
            int deletedCount = notificationService.deleteExpiredReadNotifications(now);
            log.info("읽은 알림 보관 만료 정리 완료 now={} deleted={}", now, deletedCount);
        } catch (RuntimeException e) {
            log.error("읽은 알림 보관 만료 정리 실패 now={}", now, e);
            throw e;
        }
    }
}
