package com.ibank.axwms.domain.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.notification.service.NotificationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationReadCleanupSchedulerTest {

    @Test
    @DisplayName("현재 시각으로 읽은 알림 만료 정리 서비스를 호출한다")
    void 현재_시각으로_읽은_알림_만료_정리_서비스를_호출한다() {
        NotificationService notificationService = mock(NotificationService.class);
        NotificationReadCleanupScheduler scheduler = new NotificationReadCleanupScheduler(notificationService);
        LocalDateTime before = LocalDateTime.now();

        scheduler.deleteExpiredReadNotifications();

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationService).deleteExpiredReadNotifications(nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isBetween(before, after);
    }
}
