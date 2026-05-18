package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyReadyParentProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorklogCompletionEventHandler {

    private final WorklogDependencyReadyParentFinder readyParentFinder;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 완료 저장 성공 후에만 부모 준비 알림 후보를 조회해 상태 변경 롤백과 후속 알림 부수효과를 분리한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WorklogCompletedEvent event) {
        try {
            readyParentFinder.findReadyParents(event.worklogId())
                    .forEach(parent -> publishReadyEvent(event.worklogId(), parent));
        } catch (RuntimeException exception) {
            log.warn("업무 완료 후 선행 업무 완료 알림 후보 조회에 실패했습니다. completedWorklogId={}", event.worklogId(), exception);
        }
    }

    /**
     * notification 모듈이 후속 저장만 수행하도록 worklog 판정 결과를 불변 이벤트로 변환한다.
     */
    private void publishReadyEvent(Long completedWorklogId, WorklogDependencyReadyParentProjection parent) {
        applicationEventPublisher.publishEvent(new WorklogDependencyReadyEvent(
                completedWorklogId,
                parent.parentWorklogId(),
                parent.parentAuthorId(),
                parent.parentTeamId(),
                parent.parentDepartmentId(),
                parent.teamName(),
                parent.parentTitle()
        ));
    }
}
