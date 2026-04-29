package com.ibank.axwms.domain.file.event;

import com.ibank.axwms.domain.file.external.ObjectStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 업무 첨부 파일 업로드 트랜잭션이 롤백된 직후, 트랜잭션 동안 발행된 업로드 이벤트의
 * storageKey 를 객체 스토리지에서 삭제해 고아 객체가 남지 않도록 한다.
 * 정상 commit 시에는 동작하지 않는다.
 */
@Slf4j
@Component
public class WorklogFileUploadCompensator {

    private final ObjectStoragePort objectStoragePort;

    public WorklogFileUploadCompensator(
            @Qualifier("s3ObjectStorageAdapter") ObjectStoragePort objectStoragePort
    ) {
        this.objectStoragePort = objectStoragePort;
    }

    /** AFTER_ROLLBACK 단계에서 호출되어 업로드된 객체를 개별 삭제한다. 삭제 자체 실패는 log 만 남기고 흡수한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(WorklogFileUploadedEvent event) {
        try {
            objectStoragePort.delete(event.storageKey());
        } catch (RuntimeException e) {
            log.warn("스토리지 보상 삭제 실패 key={}", event.storageKey(), e);
        }
    }
}
