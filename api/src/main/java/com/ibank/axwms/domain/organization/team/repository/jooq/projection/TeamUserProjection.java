package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

/** 팀 사용자 탭 한 행을 구성하는 read-only projection 이다. */
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
