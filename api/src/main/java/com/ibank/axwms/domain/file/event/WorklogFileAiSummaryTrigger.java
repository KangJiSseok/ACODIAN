package com.ibank.axwms.domain.file.event;

import com.ibank.axwms.domain.file.dto.TriggerFileSummaryDto;
import com.ibank.axwms.domain.file.external.FileSummaryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업무 첨부 파일이 저장된 트랜잭션이 커밋된 직후, AI 서버에 파일 1건 요약을 fire-and-forget 으로 요청한다.
 * 콜백 갱신은 별도 PATCH /internal/files/{fileId}/ai-result 경로로 들어온다.
 * AI 서버 장애로 트리거가 실패해도 사용자 요청은 이미 커밋되어 있으므로 예외는 log 만 남기고 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogFileAiSummaryTrigger {

    private final FileSummaryClient fileSummaryClient;

    /** AFTER_COMMIT 단계에서 호출되어 파일 단위 요약 요청을 1회 발사한다. 실패는 흡수하고 재시도하지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(WorklogFileAiSummaryRequestedEvent event) {
        TriggerFileSummaryDto.Request request = new TriggerFileSummaryDto.Request(
                event.fileId(),
                event.worklogId(),
                event.storageKey(),
                event.originalName(),
                event.fileExtension()
        );
        try {
            fileSummaryClient.requestSummary(request);
        } catch (RuntimeException e) {
            log.warn("AI 파일 요약 트리거 실패 fileId={} storageKey={}", event.fileId(), event.storageKey(), e);
        }
    }
}
