package com.ibank.axwms.domain.notification.scheduler;

import com.ibank.axwms.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Phase 2 범위에서 D-3 업무 알림 DB row 생성만 예약 실행하고 실시간 전파 책임은 갖지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogDueSoonReminderScheduler {

    private final NotificationService notificationService;

    /**
     * 설정된 배치 시각의 날짜를 기준으로만 알림을 만들고, Redis/SSE 전파는 이후 phase 의 별도 경계로 남긴다.
     */
    @Scheduled(
            cron = "${notification.worklog-due-soon-reminder.cron}"
    )
    public void createDueSoonReminderNotifications() {
        LocalDate today = LocalDate.now();
        try {
            int createdCount = notificationService.createWorklogDueSoonReminderNotifications(today);
            log.info("업무 마감 임박 알림 생성 완료 date={} created={}", today, createdCount);
        } catch (RuntimeException e) {
            log.error("업무 마감 임박 알림 생성 실패 date={}", today, e);
            throw e;
        }
    }
}
