package com.ibank.axwms.domain.notification.redis;

import com.ibank.axwms.domain.notification.sse.NotificationSseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 신규 알림 record 를 replay 없이 현재 서버의 SSE 연결로만 전달하는 live subscriber 다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisStreamSseSubscriber implements SmartLifecycle {

    private static final RecordId EMPTY_STREAM_OFFSET = RecordId.of("0-0");

    private final RedisConnectionFactory redisConnectionFactory;
    private final NotificationSseEmitterRegistry notificationSseEmitterRegistry;

    @Value("${notification.stream.key}")
    private String streamKey;

    @Value("${notification.stream.poll-timeout-millis}")
    private long pollTimeoutMillis;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private Subscription subscription;

    /**
     * Phase 4 는 재접속 replay 를 열지 않으므로 구독 시작 이후 들어오는 최신 record 만 읽는다.
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(pollTimeoutMillis))  //redis에 polling 주기
                        .serializer(StringRedisSerializer.UTF_8)
                        .errorHandler(error -> log.error("Redis Stream SSE 구독 오류 streamKey={}", streamKey, error))
                        .build();
        //stream  연결 컨테이너 생성
        listenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        //redis에 구독하기
        subscription = listenerContainer.receive(liveStreamOffset(), this::handleMessage);
        listenerContainer.start();
        log.info("알림 Redis Stream SSE 구독 시작 streamKey={}", streamKey);
    }

    /**
     * 애플리케이션 종료나 context 재시작 시 Redis 구독과 polling container 를 함께 닫는다.
     */
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (subscription != null && subscription.isActive()) {
                subscription.cancel();
            }
        } catch (RuntimeException e) {
            log.debug("알림 Redis Stream 구독 해제 중 오류 streamKey={}", streamKey, e);
        } finally {
            if (listenerContainer != null) {
                listenerContainer.stop();
            }
            subscription = null;
            listenerContainer = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 구독 시작 시점의 마지막 record ID 를 기준으로 고정해 listener thread 준비 지연 중 들어온 신규 record 도 놓치지 않는다.
     */
    StreamOffset<String> liveStreamOffset() {
        return StreamOffset.create(streamKey, ReadOffset.from(currentLastRecordId()));
    }

    /**
     * 기존 record 는 replay 하지 않기 위해 현재 stream 끝을 읽고, stream 이 없으면 시작 이후 첫 record 부터 받게 한다.
     */
    private RecordId currentLastRecordId() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            List<ByteRecord> records = connection.streamCommands().xRevRange(
                    StringRedisSerializer.UTF_8.serialize(streamKey),
                    Range.unbounded(),
                    Limit.limit().count(1)
            );
            if (records == null || records.isEmpty()) {
                return EMPTY_STREAM_OFFSET;
            }
            return records.getFirst().getId();
        }
    }

    /**
     * malformed record 는 버리고 정상 record 만 사용자별 SSE registry 로 넘겨 listener thread 를 보호한다.
     */
    void handleMessage(MapRecord<String, String, String> record) {
        NotificationStreamMessage.from(record)
                .ifPresentOrElse(
                        message -> {
                            int delivered = notificationSseEmitterRegistry.send(message);
                            log.debug("알림 Redis Stream SSE 전달 완료 recordId={} notificationId={} delivered={}",
                                    message.redisRecordId(), message.notificationId(), delivered);
                        },
                        () -> log.warn("알림 Redis Stream record 파싱 실패 recordId={}", record.getId())
                );
    }
}
