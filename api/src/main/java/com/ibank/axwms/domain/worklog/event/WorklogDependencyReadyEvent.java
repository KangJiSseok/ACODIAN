package com.ibank.axwms.domain.worklog.event;

/**
 * 알림 모듈이 worklog 의존성 테이블을 다시 조회하지 않도록 커밋 후 판정 결과를 snapshot 으로 고정한다.
 */
public record WorklogDependencyReadyEvent(
        Long completedWorklogId,
        Long parentWorklogId,
        Long parentAuthorId,
        Long parentTeamId,
        Long parentDepartmentId,
        String teamName,
        String parentTitle
) {
}
