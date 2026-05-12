package com.ibank.axwms.domain.notification.scheduler;

import com.ibank.axwms.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorklogOverdueReminderSchedulerTest {

    @Test
    @DisplayName("현재 날짜로 당일/초과 마감 알림 최신화 서비스를 호출한다")
    void 현재_날짜로_당일_초과_마감_알림_최신화_서비스를_호출한다() {
        // given
        NotificationService notificationService = mock(NotificationService.class);
        WorklogOverdueReminderScheduler scheduler = new WorklogOverdueReminderScheduler(notificationService);
        LocalDate today = LocalDate.now();

        // when
        scheduler.createOrUpdateOverdueReminderNotifications();

        // then
        verify(notificationService).createOrUpdateWorklogOverdueReminderNotifications(today);
    }
}
