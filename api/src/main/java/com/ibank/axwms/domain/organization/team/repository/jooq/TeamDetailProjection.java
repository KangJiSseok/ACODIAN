package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeamDetailProjection(
        Long teamId,
        String teamName,
        Long departmentId,
        String departmentName,
        TeamStatus statusCode,
        String description,
        LocalDate startDate,
        LocalDate expectedEndDate,
        LocalDateTime deletedAt,
        Long leaderUserId,
        String leaderUserName,
        String leaderPositionName,
        String leaderTitleName,
        String leaderTeamRole,
        String leaderAllocation,
        Boolean leaderIsPrimary
) {
}
