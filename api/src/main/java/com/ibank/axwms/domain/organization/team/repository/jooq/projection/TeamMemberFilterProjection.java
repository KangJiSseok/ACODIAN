package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;

import org.jooq.Record;

/**
 * 필터 옵션용 (팀, 멤버) 한 행. team LEFT JOIN user_team JOIN user 결과로,
 * 멤버가 없는 팀은 userId/userName 이 null 로 들어온다. service 가 teamId 로 그룹핑한다.
 */
public record TeamMemberFilterProjection(
        Long teamId,
        String teamName,
        Long userId,
        String userName
) {

    public static TeamMemberFilterProjection from(Record record) {
        return new TeamMemberFilterProjection(
                record.get(TB_TEAM.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_USER.USER_ID),
                record.get(TB_USER.USER_NAME)
        );
    }
}
