package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

public record TeamUserProjection(
        Boolean teamLeader,
        Long userId,
        String userName,
        String positionName,
        String email,
        String teamRole,
        String allocation
) {
}
