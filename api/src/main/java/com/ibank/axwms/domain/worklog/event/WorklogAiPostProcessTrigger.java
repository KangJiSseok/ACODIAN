package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.service.WorklogAiPostProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업무 생성 트랜잭션 커밋 이후 통합 AI 후처리를 별도 스레드에서 시작한다.
 * 사용자 응답 경로는 이미 종료된 뒤이므로 AI 서버 지연이 생성 API latency 로 전파되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogAiPostProcessTrigger {

    private final WorklogAiPostProcessService worklogAiPostProcessService;

    /** AFTER_COMMIT 이후 비동기로 통합 후처리를 실행해 롤백된 업무와 응답 지연을 동시에 차단한다. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(WorklogAiPostProcessRequestedEvent event) {
        try {
            worklogAiPostProcessService.process(event);
        } catch (RuntimeException e) {
            log.error("업무 AI 통합 후처리 실행 중 예상 밖 예외 발생 worklogId={}", event.worklogId(), e);
        }
    }
}
