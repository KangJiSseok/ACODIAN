package com.ibank.axwms.domain.worklog.event;

/**
 * 업무일지 등록 트랜잭션이 커밋된 뒤 Light v3 index 에 반영할 업무 ID 만 전달하는 이벤트다.
 */
public record WorklogLightIndexRequestedEvent(
        Long worklogId
) {
}
