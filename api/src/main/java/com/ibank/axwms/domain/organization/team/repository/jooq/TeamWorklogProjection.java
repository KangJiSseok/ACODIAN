package com.ibank.axwms.domain.organization.team.repository.jooq;

import java.time.LocalDateTime;

public record TeamWorklogProjection(
        Long worklogId,
        String title,
        String statusCode,
        Long authorUserId,
        String authorUserName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
