package com.ibank.axwms.domain.organization.team.repository.jooq.query;

/** 팀 상단 집계 조회에서 visible scope 를 고정하기 위한 내부 query 다. */
public record TeamSummaryQuery(
        Long departmentId,
        Long visibleTeamId
) {
}
