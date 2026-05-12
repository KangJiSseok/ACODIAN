package com.ibank.axwms.domain.notification.redis;

import com.ibank.axwms.domain.notification.sse.NotificationSseEmitterRegistry;
import com.ibank.axwms.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRedisStreamSseSubscriberIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Redis Stream 신규 record 는 같은 사용자 SSE 연결에만 live 전달된다")
    void Redis_Stream_신규_record는_같은_사용자_SSE_연결에만_live_전달된다() throws Exception {
        String streamKey = "notifications:test:" + System.nanoTime();
        CapturingSseEmitter targetEmitter = new CapturingSseEmitter();
        CapturingSseEmitter otherEmitter = new CapturingSseEmitter();
        NotificationSseEmitterRegistry registry = new NotificationSseEmitterRegistry(30_000L);
        NotificationRedisStreamSseSubscriber subscriber = new NotificationRedisStreamSseSubscriber(redisConnectionFactory, registry);
        ReflectionTestUtils.setField(subscriber, "streamKey", streamKey);
        ReflectionTestUtils.setField(subscriber, "pollTimeoutMillis", 100L);
        addEmitter(registry, 101L, targetEmitter);
        addEmitter(registry, 202L, otherEmitter);

        try {
            subscriber.start();
            stringRedisTemplate.opsForStream().add(streamKey, fields(101L));

            awaitSendCount(targetEmitter, 1);
            assertThat(otherEmitter.sendCount()).isZero();
        } finally {
            subscriber.stop();
            stringRedisTemplate.delete(streamKey);
        }
    }

    private static void awaitSendCount(CapturingSseEmitter emitter, int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            if (emitter.sendCount() >= expectedCount) {
                return;
            }
            Thread.sleep(50L);
        }
        assertThat(emitter.sendCount()).isGreaterThanOrEqualTo(expectedCount);
    }

    private static Map<String, String> fields(Long userId) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("notificationId", "1001");
        fields.put("userId", String.valueOf(userId));
        fields.put("type", "WORKLOG_DUE_SOON");
        fields.put("title", "업무 마감 3일 전 알림");
        fields.put("content", "마감일이 3일 남았습니다.");
        fields.put("referenceType", "WORKLOG");
        fields.put("referenceId", "501");
        fields.put("createdAt", LocalDateTime.of(2026, 5, 11, 9, 0).toString());
        return fields;
    }

    /** 통합 테스트에서도 프로덕션 registry 에 emitter 생성 hook 을 만들지 않고 현재 연결 상태만 구성한다. */
    @SuppressWarnings("unchecked")
    private static void addEmitter(NotificationSseEmitterRegistry registry, Long userId, SseEmitter emitter) {
        ConcurrentMap<Long, Set<SseEmitter>> emittersByUserId =
                (ConcurrentMap<Long, Set<SseEmitter>>) ReflectionTestUtils.getField(registry, "emittersByUserId");
        assertThat(emittersByUserId).isNotNull();
        emittersByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private final AtomicInteger sendCount = new AtomicInteger();

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sendCount.incrementAndGet();
        }

        int sendCount() {
            return sendCount.get();
        }
    }
}
