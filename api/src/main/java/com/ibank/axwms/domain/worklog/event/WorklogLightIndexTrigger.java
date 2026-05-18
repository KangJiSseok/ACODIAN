package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogLightIndexDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogLightIndexProperties;
import com.ibank.axwms.domain.worklog.external.WorklogLightIndexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업무일지 등록 커밋 직후 AI Light v3 index 를 fire-and-forget 으로 갱신 요청한다.
 * 색인 실패는 등록 성공을 되돌리지 않고 로그로만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogLightIndexTrigger {

    private final WorklogLightIndexClient worklogLightIndexClient;
    private final AiWorklogLightIndexProperties properties;

    /** AFTER_COMMIT 단계에서만 외부 AI 호출을 시작해 롤백된 업무가 Light index 에 유입되지 않도록 한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(WorklogLightIndexRequestedEvent event) {
        if (!properties.enabled()) {
            log.debug("AI Light 업무일지 index 트리거 비활성화 worklogId={}", event.worklogId());
            return;
        }

        try {
            worklogLightIndexClient.requestIndex(TriggerWorklogLightIndexDto.Request.of(event.worklogId()));
        } catch (RuntimeException e) {
            log.warn("AI Light 업무일지 index 트리거 실패 worklogId={}", event.worklogId(), e);
        }
    }
}
