package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/**
 * 선행 업무 대기 위젯의 한 행. (blocked worklog → predecessor) 쌍을 평면적으로 담는다.
 * service 가 myWorklogId 로 그룹핑해 응답으로 가공한다.
 */
public record BlockedPredecessorRowProjection(
        Long myWorklogId,
        String myTitle,
        Long predecessorWorklogId,
        String predecessorTitle,
        String predecessorStatusCode
) {
}
