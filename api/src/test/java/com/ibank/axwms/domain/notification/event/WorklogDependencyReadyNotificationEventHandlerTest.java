package com.ibank.axwms.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.domain.worklog.event.WorklogDependencyReadyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorklogDependencyReadyNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorklogDependencyReadyNotificationEventHandler handler;

    @Test
    @DisplayName("ready snapshot 이벤트를 선행 업무 완료 알림 생성 요청으로 변환한다")
    void ready_snapshot_이벤트를_선행_업무_완료_알림_생성_요청으로_변환한다() {
        // given
        WorklogDependencyReadyEvent event = readyEvent();

        // when
        handler.handle(event);

        // then
        verify(notificationService).createWorklogDependencyReadyNotification(
                101L,
                3L,
                21L,
                701L,
                "물류혁신TF",
                "부모 업무"
        );
    }

    @Test
    @DisplayName("알림 생성 실패는 완료 상태 변경 이후 흐름으로 전파하지 않는다")
    void 알림_생성_실패는_완료_상태_변경_이후_흐름으로_전파하지_않는다() {
        // given
        WorklogDependencyReadyEvent event = readyEvent();
        doThrow(new RuntimeException("notification failed")).when(notificationService)
                .createWorklogDependencyReadyNotification(101L, 3L, 21L, 701L, "물류혁신TF", "부모 업무");

        // when & then
        assertThatCode(() -> handler.handle(event)).doesNotThrowAnyException();
    }

    private WorklogDependencyReadyEvent readyEvent() {
        return new WorklogDependencyReadyEvent(
                501L,
                701L,
                101L,
                21L,
                3L,
                "물류혁신TF",
                "부모 업무"
        );
    }
}
