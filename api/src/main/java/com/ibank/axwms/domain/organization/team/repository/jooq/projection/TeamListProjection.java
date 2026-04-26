package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import java.time.LocalDate;

public record TeamListProjection(
        Long teamId,
        String teamName,
        TeamStatus statusCode,
        Long departmentId,
        String departmentName,
        String description,
        Long departmentHeadUserId,
        String departmentHeadUserName,
        Long teamLeaderId,
        String teamLeaderName,
        Integer memberCount,
        Boolean myTeamLeader,
        String teamRole,
        String allocation,
        Boolean isPrimary,
        LocalDate startDate,
        LocalDate expectedEndDate
) {
}
