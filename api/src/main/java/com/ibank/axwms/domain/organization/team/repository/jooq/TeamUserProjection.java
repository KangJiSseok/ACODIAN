package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;

public record TeamUserProjection(
        Long userId,
        String userName,
        String positionName,
        String titleName,
        Boolean teamLeader,
        String teamRole,
        String allocation,
        Boolean isPrimary,
        UserTeamStatus statusCode
) {
}
