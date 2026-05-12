package com.ibank.axwms.domain.file.event;

/**
 * 업무 첨부 파일이 업로드되어 DB 에 저장된 직후 활성 트랜잭션 안에서 발행되는 이벤트.
 * AFTER_COMMIT listener 가 AI 서버에 파일 요약을 요청한다.
 * 트랜잭션이 롤백되면 listener 가 발화되지 않아 요약 요청이 나가지 않는다.
 */
public record WorklogFileAiSummaryRequestedEvent(
        Long fileId,
        Long worklogId,
        String storageKey,
        String originalName,
        String fileExtension
) {
}
