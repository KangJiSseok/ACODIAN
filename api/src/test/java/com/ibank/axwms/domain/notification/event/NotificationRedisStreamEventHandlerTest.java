package com.ibank.axwms.domain.notification.event;

import com.ibank.axwms.domain.notification.redis.NotificationRedisStreamPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationRedisStreamEventHandlerTest {

    @Test
    @DisplayName("Redis Stream 발행 실패를 호출자에게 전파하지 않는다")
    void Redis_Stream_발행_실패를_호출자에게_전파하지_않는다() {
        // given
        NotificationRedisStreamPublisher publisher = mock(NotificationRedisStreamPublisher.class);
        NotificationRedisStreamEventHandler handler = new NotificationRedisStreamEventHandler(publisher);
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                1L,
                2L,
                "WORKLOG_DUE_SOON",
                "업무 마감 3일 전 알림",
                "마감일이 3일 남았습니다.",
                "WORKLOG",
                3L,
                LocalDateTime.of(2026, 5, 11, 9, 0)
        );
        doThrow(new RuntimeException("redis down")).when(publisher).publish(event);

        // when & then
        assertThatNoException().isThrownBy(() -> handler.handle(event));
        verify(publisher).publish(event);
    }
}
