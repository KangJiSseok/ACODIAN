package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

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
