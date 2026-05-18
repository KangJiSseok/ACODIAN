package com.ibank.axwms.domain.worklog.event;

/**
 * 업무일지 등록 트랜잭션이 커밋된 뒤 본문 요약과 태그 생성 파이프라인을 시작하기 위한 이벤트.
 * AI 콜백이 DB 를 다시 갱신하므로 롤백된 업무에 대해 외부 파이프라인이 먼저 실행되지 않도록 AFTER_COMMIT listener 로만 소비한다.
 */
public record WorklogAiPipelineRequestedEvent(
        Long worklogId,
        String requestContent,
        String workContent,
        Long authorId,
        Long teamId,
        Long departmentId
) {
}
