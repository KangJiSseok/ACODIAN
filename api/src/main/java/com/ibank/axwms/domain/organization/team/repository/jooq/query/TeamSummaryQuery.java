package com.ibank.axwms.domain.organization.team.repository.jooq.query;

public record TeamSummaryQuery(
        Long departmentId,
        Long visibleTeamId
) {
}
