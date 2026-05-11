package com.ibank.axwms.domain.notification.redis;

import com.ibank.axwms.domain.notification.event.NotificationCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRedisStreamPublisherTest {

    private static final String STREAM_KEY = "notifications:stream";

    @Test
    @DisplayName("Redis Stream 발행 field 는 필수값을 원본으로 두고 선택값만 빈 문자열로 표현한다")
    void Redis_Stream_발행_field는_필수값을_원본으로_두고_선택값만_빈_문자열로_표현한다() {
        // given
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> streamOperations = streamOperations(stringRedisTemplate);
        when(streamOperations.add(eq(STREAM_KEY), anyMap())).thenReturn(RecordId.of("1-0"));
        NotificationRedisStreamPublisher publisher = new NotificationRedisStreamPublisher(stringRedisTemplate, STREAM_KEY);
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                1001L,
                101L,
                "WORKLOG_DUE_SOON",
                "업무 마감 3일 전 알림",
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 11, 9, 0)
        );

        // when
        publisher.publish(event);

        // then
        Map<String, String> fields = captureFields(streamOperations);
        assertThat(fields)
                .containsEntry("notificationId", "1001")
                .containsEntry("userId", "101")
                .containsEntry("type", "WORKLOG_DUE_SOON")
                .containsEntry("title", "업무 마감 3일 전 알림")
                .containsEntry("content", "")
                .containsEntry("referenceType", "")
                .containsEntry("referenceId", "")
                .containsEntry("createdAt", "2026-05-11T09:00");
    }

    @Test
    @DisplayName("필수 문자열 field null 은 빈 문자열로 숨기지 않는다")
    void 필수_문자열_field_null은_빈_문자열로_숨기지_않는다() {
        // given
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> streamOperations = streamOperations(stringRedisTemplate);
        when(streamOperations.add(eq(STREAM_KEY), anyMap())).thenReturn(RecordId.of("1-0"));
        NotificationRedisStreamPublisher publisher = new NotificationRedisStreamPublisher(stringRedisTemplate, STREAM_KEY);
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                1001L,
                101L,
                null,
                null,
                "마감일이 3일 남았습니다.",
                "WORKLOG",
                501L,
                LocalDateTime.of(2026, 5, 11, 9, 0)
        );

        // when
        publisher.publish(event);

        // then
        Map<String, String> fields = captureFields(streamOperations);
        assertThat(fields).containsEntry("type", null)
                .containsEntry("title", null)
                .containsEntry("createdAt", "2026-05-11T09:00");
    }

    /** 테스트가 관심 있는 Redis Stream field map 만 캡처하도록 Redis template generic 차이를 한곳에 격리한다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static StreamOperations<String, Object, Object> streamOperations(StringRedisTemplate stringRedisTemplate) {
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        when(stringRedisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        return streamOperations;
    }

    /** 발행자가 Redis Stream 으로 넘긴 field 계약을 검증하기 위해 mock 호출 인자를 복원한다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, String> captureFields(StreamOperations<String, Object, Object> streamOperations) {
        ArgumentCaptor<Map> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(STREAM_KEY), fieldsCaptor.capture());
        return (Map<String, String>) fieldsCaptor.getValue();
    }
}
