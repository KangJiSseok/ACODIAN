package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import org.jooq.Record;

/** visible scope 팀의 운영 상태별 개수를 담는 읽기 전용 projection 이다. */
public record TeamStatusSummaryProjection(
        Long activeTeamCount,
        Long inactiveTeamCount,
        Long totalTeamCount
) {

    /** JOOQ 조회 결과 record 를 팀 상태 요약 projection 으로 조립한다. */
    public static TeamStatusSummaryProjection from(Record record) {
        return new TeamStatusSummaryProjection(
                record.get("active_team_count", Long.class),
                record.get("inactive_team_count", Long.class),
                record.get("total_team_count", Long.class)
        );
    }
}
