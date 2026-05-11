package com.ibank.axwms.domain.notification.redis;

import com.ibank.axwms.domain.notification.event.NotificationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 커밋된 Notification snapshot 을 Redis Stream record 로 변환해 발행한다.
 */
@Slf4j
@Component
public class NotificationRedisStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final String streamKey;

    /**
     * Phase 3 에서는 Redis Stream key 하나만 외부 설정으로 열어 두고 별도 설정 객체는 두지 않는다.
     */
    public NotificationRedisStreamPublisher(
            StringRedisTemplate stringRedisTemplate,
            @Value("${notification.stream.key}") String streamKey
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.streamKey = streamKey;
    }

    /**
     * Redis 가 생성한 RecordId 는 관측용으로만 반환하고 Notification 식별자는 field 로 별도 유지한다.
     */
    public RecordId publish(NotificationCreatedEvent event) {
        RecordId recordId = stringRedisTemplate.opsForStream()
                .add(streamKey, toStreamFields(event));
        log.debug("Redis Stream 알림 발행 완료 streamKey={} recordId={} notificationId={}",
                streamKey, recordId, event.notificationId());
        return recordId;
    }

    /**
     * 필수 field 는 원본 계약을 그대로 드러내고, 선택 field 만 Redis hash field 유지를 위해 빈 문자열로 표현한다.
     */
    private Map<String, String> toStreamFields(NotificationCreatedEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("notificationId", event.notificationId().toString());
        fields.put("userId", event.userId().toString());
        fields.put("type", event.type());
        fields.put("title", event.title());
        fields.put("content", nullToEmpty(event.content()));
        fields.put("referenceType", nullToEmpty(event.referenceType()));
        fields.put("referenceId", event.referenceId() == null ? "" : String.valueOf(event.referenceId()));
        fields.put("createdAt", event.createdAt().toString());
        return fields;
    }

    /**
     * 선택값 누락을 Redis hash field 누락이 아니라 빈 문자열 계약으로 표현한다.
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
