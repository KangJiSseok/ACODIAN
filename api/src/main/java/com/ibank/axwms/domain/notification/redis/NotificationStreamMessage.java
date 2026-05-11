package com.ibank.axwms.domain.notification.redis;

import org.springframework.data.redis.connection.stream.MapRecord;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Redis Stream 문자열 field 계약을 SSE 전달 전에 타입이 있는 내부 메시지로 고정한다.
 */
public record NotificationStreamMessage(
        String redisRecordId,
        Long notificationId,
        Long userId,
        String type,
        String title,
        String content,
        String referenceType,
        Long referenceId,
        LocalDateTime createdAt
) {

    /**
     * Redis listener 가 잘못된 record 를 전체 구독 실패로 전파하지 않도록 파싱 실패를 빈 결과로 격리한다.
     */
    public static Optional<NotificationStreamMessage> from(MapRecord<String, String, String> record) {
        try {
            Map<String, String> fields = record.getValue();
            return Optional.of(new NotificationStreamMessage(
                    record.getId().getValue(),
                    requiredLong(fields, "notificationId"),
                    requiredLong(fields, "userId"),
                    requiredString(fields, "type"),
                    requiredString(fields, "title"),
                    emptyToNull(fields.get("content")),
                    emptyToNull(fields.get("referenceType")),
                    optionalLong(fields.get("referenceId")),
                    LocalDateTime.parse(requiredString(fields, "createdAt"))
            ));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** 필수 숫자 field 는 SSE 라우팅과 이벤트 식별에 필요하므로 누락/공백을 모두 파싱 실패로 본다. */
    private static Long requiredLong(Map<String, String> fields, String name) {
        return Long.parseLong(requiredString(fields, name));
    }

    /** 필수 문자열 field 는 빈 문자열도 의미 없는 payload 로 간주해 잘못된 stream record 를 버리게 한다. */
    private static String requiredString(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required stream field: " + name);
        }
        return value;
    }

    /** 선택 숫자 field 는 빈 문자열을 null 로 복원한다. */
    private static Long optionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    /** 선택 문자열 field 는 Redis hash field 유지를 위한 빈 문자열 표현을 API payload 에서는 null 로 되돌린다. */
    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
