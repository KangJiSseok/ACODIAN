package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

/** 팀 업무일지 탭 한 행을 구성하는 read-only projection 이다. */
public record TeamWorklogProjection(
        Long worklogId,
        String title,
        String requestContent,
        String workContent,
        String aiSummary,
        String statusCode,
        String importanceCode
) {
}
