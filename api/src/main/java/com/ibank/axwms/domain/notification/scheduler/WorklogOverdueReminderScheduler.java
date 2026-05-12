package com.ibank.axwms.domain.notification.scheduler;

import com.ibank.axwms.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 당일/초과 마감 업무 알림을 하루 1회 저장/update 하되 실시간 publish 경계는 갖지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogOverdueReminderScheduler {

    private final NotificationService notificationService;

    /**
     * 설정된 배치 시각의 날짜를 기준으로 당일/초과 마감 알림 row 만 최신화한다.
     */
    @Scheduled(
            cron = "${notification.worklog-overdue-reminder.cron}"
    )
    public void createOrUpdateOverdueReminderNotifications() {
        LocalDate today = LocalDate.now();
        try {
            int affectedCount = notificationService.createOrUpdateWorklogOverdueReminderNotifications(today);
            log.info("업무 당일/초과 마감 알림 최신화 완료 date={} affected={}", today, affectedCount);
        } catch (RuntimeException e) {
            log.error("업무 당일/초과 마감 알림 최신화 실패 date={}", today, e);
            throw e;
        }
    }
}
