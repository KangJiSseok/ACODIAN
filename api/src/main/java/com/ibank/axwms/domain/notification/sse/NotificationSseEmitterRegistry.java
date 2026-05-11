package com.ibank.axwms.domain.notification.sse;

import com.ibank.axwms.domain.notification.dto.SubscribeNotificationStreamApiDto;
import com.ibank.axwms.domain.notification.redis.NotificationStreamMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 사용자별 SSE 연결 생명주기를 관리하고 Redis 알림 메시지를 해당 사용자 연결에만 fan-out 한다.
 */
@Slf4j
@Component
public class NotificationSseEmitterRegistry {

    private static final String CONNECT_EVENT_NAME = "connected";
    private static final String HEARTBEAT_EVENT_NAME = "heartbeat";
    private static final String NOTIFICATION_EVENT_NAME = "notification";

    private final ConcurrentMap<Long, Set<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();
    private final long timeoutMillis;

    /**
     * SSE timeout 은 HTTP 연결 수명만 제어하며 재접속 replay 정책은 Phase 5 전까지 열지 않는다.
     */
    public NotificationSseEmitterRegistry(@Value("${notification.sse.timeout-millis}") long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 인증된 사용자 한 명의 SSE 연결을 등록하고 정상/오류/timeout 종료 시 registry 에서 제거되게 한다.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emittersByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> timeout(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        sendLifecycleEvent(userId, emitter, CONNECT_EVENT_NAME, "connected");
        return emitter;
    }

    /**
     * Redis Stream 에서 복원한 사용자 식별자로 같은 사용자의 현재 SSE 연결에만 실시간 알림을 전달한다.
     */
    public int send(NotificationStreamMessage message) {
        Set<SseEmitter> emitters = emittersByUserId.get(message.userId());
        if (emitters == null || emitters.isEmpty()) {
            return 0;
        }
        SubscribeNotificationStreamApiDto.Response response = SubscribeNotificationStreamApiDto.Response.from(message);
        int deliveredCount = 0;
        for (SseEmitter emitter : emitters) {
            if (sendNotification(message, response, emitter)) {
                deliveredCount++;
            }
        }
        return deliveredCount;
    }

    /**
     * 프록시와 브라우저가 유휴 SSE 연결을 끊지 않도록 현재 열린 연결에만 heartbeat 를 보낸다.
     */
    @Scheduled(fixedDelayString = "${notification.sse.heartbeat-delay-millis}")
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, emitters) -> emitters.forEach(emitter ->
                sendLifecycleEvent(userId, emitter, HEARTBEAT_EVENT_NAME, "keep-alive")
        ));
    }


    /** SSE 전송 실패는 끊어진 연결로 보고 registry 에서 제거해 이후 fan-out 비용과 오류를 줄인다. */
    private boolean sendNotification(
            NotificationStreamMessage message,
            SubscribeNotificationStreamApiDto.Response response,
            SseEmitter emitter
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .id(message.redisRecordId())
                    .name(NOTIFICATION_EVENT_NAME)
                    .data(response));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("알림 SSE 전송 실패 userId={} notificationId={}", message.userId(), message.notificationId(), e);
            remove(message.userId(), emitter);
            emitter.completeWithError(e);
            return false;
        }
    }

    /** 연결 확인/heartbeat 이벤트는 payload 없이 연결 유지 목적만 가지므로 실패 시 해당 emitter 만 정리한다. */
    private void sendLifecycleEvent(Long userId, SseEmitter emitter, String eventName, String comment) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .comment(comment));
        } catch (IOException | IllegalStateException e) {
            log.debug("알림 SSE 생명주기 이벤트 전송 실패 userId={} event={}", userId, eventName, e);
            remove(userId, emitter);
            emitter.completeWithError(e);
        }
    }

    /** timeout 된 연결은 서버가 명시적으로 완료해 클라이언트 재접속 판단을 단순화한다. */
    private void timeout(Long userId, SseEmitter emitter) {
        remove(userId, emitter);
        emitter.complete();
    }

    /** 사용자별 emitter set 이 비면 map entry 도 제거해 장시간 운영 중 사용자 key 누수를 막는다. */
    private void remove(Long userId, SseEmitter emitter) {
        emittersByUserId.computeIfPresent(userId, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
