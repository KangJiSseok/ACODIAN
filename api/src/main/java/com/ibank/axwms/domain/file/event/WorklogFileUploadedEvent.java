package com.ibank.axwms.domain.file.event;

/**
 * 업무 첨부 파일이 객체 스토리지에 업로드된 직후 활성 트랜잭션 안에서 발행되는 이벤트.
 * 트랜잭션이 롤백되면 listener 가 storageKey 를 보상 삭제해 고아 객체가 남지 않도록 한다.
 */
public record WorklogFileUploadedEvent(String storageKey) {
}
