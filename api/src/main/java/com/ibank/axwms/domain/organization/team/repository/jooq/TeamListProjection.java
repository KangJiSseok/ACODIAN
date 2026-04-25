package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import java.time.LocalDateTime;

public record TeamListProjection(
        Long teamId,
        String teamName,
        Long departmentId,
        String departmentName,
        TeamStatus statusCode,
        LocalDateTime deletedAt,
        Long leaderUserId,
        String leaderUserName,
        Integer memberCount
) {
}
