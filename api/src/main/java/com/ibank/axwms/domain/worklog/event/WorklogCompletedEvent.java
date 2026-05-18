package com.ibank.axwms.domain.worklog.event;

/**
 * 상태 변경 트랜잭션과 후속 의존성 판정을 분리하기 위해 완료된 업무 ID 만 전달하는 커밋 전 이벤트다.
 */
public record WorklogCompletedEvent(
        Long worklogId
) {
}
