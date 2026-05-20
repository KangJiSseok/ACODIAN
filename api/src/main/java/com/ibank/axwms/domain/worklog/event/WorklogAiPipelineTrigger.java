package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogPipelineDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogPipelineProperties;
import com.ibank.axwms.domain.worklog.external.WorklogPipelineClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업무일지 등록 커밋 직후 AI 서버에 본문 요약/태그 생성 파이프라인을 fire-and-forget 으로 요청한다.
 * AI 서버 장애는 등록 성공을 되돌리지 않으며, 결과 반영은 내부 콜백 API 가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogAiPipelineTrigger {

    private final WorklogPipelineClient worklogPipelineClient;
    private final AiWorklogPipelineProperties properties;

    /** AFTER_COMMIT 단계에서만 외부 AI 호출을 시작해 롤백된 업무가 AI 파이프라인에 유입되지 않도록 한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(WorklogAiPipelineRequestedEvent event) {
        if (!properties.enabled()) {
            log.debug("AI 업무일지 파이프라인 트리거 비활성화 worklogId={}", event.worklogId());
            return;
        }

        TriggerWorklogPipelineDto.Request request = new TriggerWorklogPipelineDto.Request(
                event.worklogId(),
                event.requestContent(),
                event.workContent(),
                event.authorId(),
                event.teamId(),
                event.departmentId()
        );
        try {
            worklogPipelineClient.requestPipeline(request);
        } catch (RuntimeException e) {
            log.warn("AI 업무일지 파이프라인 트리거 실패 worklogId={}", event.worklogId(), e);
        }
    }
}
