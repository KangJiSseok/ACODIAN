package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import java.time.LocalDate;

/** 팀 상세 화면이 service 계층으로 전달받는 read-only projection 이다. */
public record TeamDetailProjection(
        Long teamId,
        String teamName,
        TeamStatus statusCode,
        String description,
        Long departmentId,
        String departmentName,
        Long teamLeaderId,
        String teamLeaderName,
        LocalDate startDate,
        LocalDate expectedEndDate,
        Long totalWorklogCount,
        Long completedWorklogCount
) {
}
