package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;

/** user_team 과 team 을 조인한 읽기 전용 사용자-팀 소속 projection 이다. */
public record UserTeamSummaryProjection(
        Boolean isPrimary,
        Long teamId,
        String teamName,
        Boolean isLeader,
        String teamRole,
        String allocation
) {

    /** jOOQ 조회 결과 record 를 사용자-팀 소속 projection 으로 조립한다. */
    public static UserTeamSummaryProjection from(Record record) {
        return new UserTeamSummaryProjection(
                record.get(TB_USER_TEAM.IS_PRIMARY),
                record.get(TB_TEAM.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_USER_TEAM.IS_LEADER),
                record.get(TB_USER_TEAM.TEAM_ROLE),
                record.get(TB_USER_TEAM.ALLOCATION)
        );
    }
}
