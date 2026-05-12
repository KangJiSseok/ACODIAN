package com.ibank.axwms.domain.notification.scheduler;

import com.ibank.axwms.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorklogDueSoonReminderSchedulerTest {

    @Test
    @DisplayName("현재 날짜로 D-3 알림 생성 서비스를 호출한다")
    void 현재_날짜로_D3_알림_생성_서비스를_호출한다() {
        // given
        NotificationService notificationService = mock(NotificationService.class);
        WorklogDueSoonReminderScheduler scheduler = new WorklogDueSoonReminderScheduler(notificationService);
        LocalDate today = LocalDate.now();

        // when
        scheduler.createDueSoonReminderNotifications();

        // then
        verify(notificationService).createWorklogDueSoonReminderNotifications(today);
    }
}
