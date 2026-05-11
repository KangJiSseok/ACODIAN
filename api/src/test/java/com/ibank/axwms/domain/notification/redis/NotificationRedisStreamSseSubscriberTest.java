package com.ibank.axwms.domain.notification.redis;

import com.ibank.axwms.domain.notification.sse.NotificationSseEmitterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationRedisStreamSseSubscriberTest {

    @Test
    @DisplayName("정상 Redis Stream record 는 SSE registry 로 전달한다")
    void 정상_Redis_Stream_record는_SSE_registry로_전달한다() {
        NotificationSseEmitterRegistry registry = mock(NotificationSseEmitterRegistry.class);
        NotificationRedisStreamSseSubscriber subscriber = new NotificationRedisStreamSseSubscriber(null, registry);

        subscriber.handleMessage(record(fields()));

        verify(registry).send(argThat(message ->
                message.notificationId().equals(1001L) && message.userId().equals(101L)));
    }

    @Test
    @DisplayName("잘못된 Redis Stream record 는 SSE registry 로 전달하지 않는다")
    void 잘못된_Redis_Stream_record는_SSE_registry로_전달하지_않는다() {
        NotificationSseEmitterRegistry registry = mock(NotificationSseEmitterRegistry.class);
        NotificationRedisStreamSseSubscriber subscriber = new NotificationRedisStreamSseSubscriber(null, registry);
        Map<String, String> fields = fields();
        fields.put("notificationId", "invalid");

        subscriber.handleMessage(record(fields));

        verify(registry, never()).send(any());
    }

    @Test
    @DisplayName("live 구독 offset 은 첫 read 이후 마지막 수신 ID 기준으로 전진한다")
    void live_구독_offset은_첫_read_이후_마지막_수신_ID_기준으로_전진한다() {
        NotificationRedisStreamSseSubscriber subscriber = new NotificationRedisStreamSseSubscriber(null, mock(NotificationSseEmitterRegistry.class));
        ReflectionTestUtils.setField(subscriber, "streamKey", "notifications:stream");

        org.springframework.data.redis.connection.stream.StreamOffset<String> offset = subscriber.liveStreamOffset();

        assertThat(offset.getKey()).isEqualTo("notifications:stream");
        assertThat(offset.getOffset()).isEqualTo(ReadOffset.lastConsumed());
    }

    @Test
    @DisplayName("stop 은 시작 전에도 안전하게 no-op 처리한다")
    void stop은_시작_전에도_안전하게_noop_처리한다() {
        NotificationRedisStreamSseSubscriber subscriber = new NotificationRedisStreamSseSubscriber(null, mock(NotificationSseEmitterRegistry.class));
        ReflectionTestUtils.setField(subscriber, "streamKey", "notifications:stream");

        subscriber.stop();

        assertThat(subscriber.isRunning()).isFalse();
    }

    private static MapRecord<String, String, String> record(Map<String, String> fields) {
        return MapRecord.create("notifications:stream", fields).withId(RecordId.of("1-0"));
    }

    private static Map<String, String> fields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("notificationId", "1001");
        fields.put("userId", "101");
        fields.put("type", "WORKLOG_DUE_SOON");
        fields.put("title", "업무 마감 3일 전 알림");
        fields.put("content", "마감일이 3일 남았습니다.");
        fields.put("referenceType", "WORKLOG");
        fields.put("referenceId", "501");
        fields.put("createdAt", "2026-05-11T09:00:00");
        return fields;
    }
}
