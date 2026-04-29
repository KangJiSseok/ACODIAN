package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamAuthority;
import java.time.LocalDate;

/** 팀 목록 화면 한 행을 구성하는 read-only projection 이다. */
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
        UserTeamAuthority myTeamAuthority,
        String teamRole,
        String allocation,
        Boolean isPrimary,
        LocalDate startDate,
        LocalDate expectedEndDate
) {
}
