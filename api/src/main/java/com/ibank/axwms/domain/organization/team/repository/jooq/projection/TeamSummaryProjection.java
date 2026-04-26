package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

public record TeamSummaryProjection(
        Long activeTeamCount,
        Long totalTeamCount,
        Long activeUserCount,
        Long activeTeamUserCount,
        Long allTeamUserCount
) {

    public static TeamSummaryProjection empty() {
        return new TeamSummaryProjection(0L, 0L, 0L, 0L, 0L);
    }
}
