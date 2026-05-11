package com.ibank.axwms.domain.notification.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStreamMessageTest {

    @Test
    @DisplayName("Redis Stream 문자열 field 를 SSE 내부 메시지로 복원한다")
    void Redis_Stream_문자열_field를_SSE_내부_메시지로_복원한다() {
        MapRecord<String, String, String> record = record(fields());

        Optional<NotificationStreamMessage> result = NotificationStreamMessage.from(record);

        assertThat(result).hasValueSatisfying(message -> {
            assertThat(message.redisRecordId()).isEqualTo("1-0");
            assertThat(message.notificationId()).isEqualTo(1001L);
            assertThat(message.userId()).isEqualTo(101L);
            assertThat(message.type()).isEqualTo("WORKLOG_DUE_SOON");
            assertThat(message.title()).isEqualTo("업무 마감 3일 전 알림");
            assertThat(message.content()).isEqualTo("마감일이 3일 남았습니다.");
            assertThat(message.referenceType()).isEqualTo("WORKLOG");
            assertThat(message.referenceId()).isEqualTo(501L);
            assertThat(message.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 11, 9, 0));
        });
    }

    @Test
    @DisplayName("필수 field 가 누락된 Redis Stream record 는 빈 결과로 격리한다")
    void 필수_field가_누락된_Redis_Stream_record는_빈_결과로_격리한다() {
        Map<String, String> fields = fields();
        fields.remove("userId");

        Optional<NotificationStreamMessage> result = NotificationStreamMessage.from(record(fields));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("선택 field 의 빈 문자열은 null 로 복원한다")
    void 선택_field의_빈_문자열은_null로_복원한다() {
        Map<String, String> fields = fields();
        fields.put("content", "");
        fields.put("referenceType", "");
        fields.put("referenceId", "");

        Optional<NotificationStreamMessage> result = NotificationStreamMessage.from(record(fields));

        assertThat(result).hasValueSatisfying(message -> {
            assertThat(message.content()).isNull();
            assertThat(message.referenceType()).isNull();
            assertThat(message.referenceId()).isNull();
        });
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
