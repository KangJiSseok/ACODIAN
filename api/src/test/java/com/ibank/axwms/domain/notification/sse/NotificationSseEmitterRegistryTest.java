package com.ibank.axwms.domain.notification.sse;

import com.ibank.axwms.domain.notification.redis.NotificationStreamMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSseEmitterRegistryTest {

    @Test
    @DisplayName("알림 메시지는 같은 userId 의 SSE 연결에만 전달한다")
    void 알림_메시지는_같은_userId의_SSE_연결에만_전달한다() {
        NotificationSseEmitterRegistry registry = new NotificationSseEmitterRegistry(30_000L);
        CapturingSseEmitter targetEmitter = new CapturingSseEmitter();
        CapturingSseEmitter otherEmitter = new CapturingSseEmitter();
        addEmitter(registry, 101L, targetEmitter);
        addEmitter(registry, 202L, otherEmitter);

        int deliveredCount = registry.send(message(101L));

        assertThat(deliveredCount).isEqualTo(1);
        assertThat(targetEmitter.sendCount()).isEqualTo(1);
        assertThat(otherEmitter.sendCount()).isZero();
    }

    @Test
    @DisplayName("전송 실패한 SSE 연결은 사용자 registry 에서 제거한다")
    void 전송_실패한_SSE_연결은_사용자_registry에서_제거한다() {
        NotificationSseEmitterRegistry registry = new NotificationSseEmitterRegistry(30_000L);
        addEmitter(registry, 101L, new FailingSseEmitter());

        int deliveredCount = registry.send(message(101L));

        assertThat(deliveredCount).isZero();
        assertThat(activeEmitterCount(registry, 101L)).isZero();
    }

    @Test
    @DisplayName("heartbeat 는 현재 열린 모든 SSE 연결에 전송한다")
    void heartbeat는_현재_열린_모든_SSE_연결에_전송한다() {
        NotificationSseEmitterRegistry registry = new NotificationSseEmitterRegistry(30_000L);
        CapturingSseEmitter firstEmitter = new CapturingSseEmitter();
        CapturingSseEmitter secondEmitter = new CapturingSseEmitter();
        addEmitter(registry, 101L, firstEmitter);
        addEmitter(registry, 202L, secondEmitter);

        registry.sendHeartbeat();

        assertThat(firstEmitter.sendCount()).isEqualTo(1);
        assertThat(secondEmitter.sendCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("register 는 사용자 registry 에 SSE 연결을 추가한다")
    void register는_사용자_registry에_SSE_연결을_추가한다() {
        NotificationSseEmitterRegistry registry = new NotificationSseEmitterRegistry(30_000L);

        SseEmitter emitter = registry.register(101L);

        assertThat(emitter).isNotNull();
        assertThat(activeEmitterCount(registry, 101L)).isEqualTo(1);
    }

    private static NotificationStreamMessage message(Long userId) {
        return new NotificationStreamMessage(
                "1-0",
                1001L,
                userId,
                "WORKLOG_DUE_SOON",
                "업무 마감 3일 전 알림",
                "마감일이 3일 남았습니다.",
                "WORKLOG",
                501L,
                LocalDateTime.of(2026, 5, 11, 9, 0)
        );
    }

    /** 프로덕션 registry 에 테스트 전용 주입 hook 을 두지 않기 위해 private 저장소를 테스트에서만 조작한다. */
    private static void addEmitter(NotificationSseEmitterRegistry registry, Long userId, SseEmitter emitter) {
        emittersByUserId(registry)
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(emitter);
    }

    /** 테스트가 검증할 사용자별 연결 수를 production API 로 노출하지 않도록 reflection 으로만 읽는다. */
    private static int activeEmitterCount(NotificationSseEmitterRegistry registry, Long userId) {
        Set<SseEmitter> emitters = emittersByUserId(registry).get(userId);
        return emitters == null ? 0 : emitters.size();
    }

    /** 테스트 전용 접근을 한곳에 모아 registry 의 공개 표면이 테스트 요구로 넓어지지 않게 한다. */
    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Long, Set<SseEmitter>> emittersByUserId(NotificationSseEmitterRegistry registry) {
        try {
            Field field = NotificationSseEmitterRegistry.class.getDeclaredField("emittersByUserId");
            field.setAccessible(true);
            return (ConcurrentMap<Long, Set<SseEmitter>>) field.get(registry);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SSE emitter registry 내부 저장소 접근 실패", e);
        }
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private int sendCount;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sendCount++;
        }

        int sendCount() {
            return sendCount;
        }
    }

    private static class FailingSseEmitter extends CapturingSseEmitter {
        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("disconnected");
        }
    }
}
