package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

/** 팀 목록 상단 KPI 블록을 구성하는 read-only projection 이다. */
public record TeamSummaryProjection(
        Long activeTeamCount,
        Long totalTeamCount,
        Long activeUserCount,
        Long activeTeamUserCount,
        Long allTeamUserCount
) {

    /** 결과가 비어 있을 때 service 가 null 대신 사용할 기본 집계값을 만든다. */
    public static TeamSummaryProjection empty() {
        return new TeamSummaryProjection(0L, 0L, 0L, 0L, 0L);
    }
}
