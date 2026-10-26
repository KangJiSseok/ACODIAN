package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.external.AiWorklogGraphIndexProperties;
import com.ibank.axwms.domain.worklog.external.WorklogGraphIndexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 업무일지 생성·수정 커밋 후 내부 AI 큐에 GraphRAG 색인을 접수한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogGraphIndexTrigger {
    private final WorklogGraphIndexClient client;
    private final AiWorklogGraphIndexProperties properties;

    /** 롤백된 업무를 색인하지 않고 HTTP 요청 지연을 사용자 응답에서 분리한다. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(WorklogGraphIndexRequestedEvent event) {
        if (!properties.enabled()) {
            return;
        }
        try {
            client.requestIndex(event.worklogId(), event.refreshEmbedding());
        } catch (RuntimeException e) {
            log.warn("GraphRAG index enqueue failed worklogId={}", event.worklogId(), e);
        }
    }
}
