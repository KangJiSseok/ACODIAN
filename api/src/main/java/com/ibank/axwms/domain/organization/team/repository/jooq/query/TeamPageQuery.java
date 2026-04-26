package com.ibank.axwms.domain.organization.team.repository.jooq.query;

public record TeamPageQuery(
        int page,
        int pageSize,
        Long departmentId,
        Long visibleTeamId,
        Long principalUserId
) {
}
