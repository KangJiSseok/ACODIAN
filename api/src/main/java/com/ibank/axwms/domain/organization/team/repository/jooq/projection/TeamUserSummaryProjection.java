package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import org.jooq.Record;

/** 팀 사용자 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record TeamUserSummaryProjection(
        Boolean isLeader,
        Long userId,
        String userName,
        String positionName,
        String teamRole
) {

    /** JOOQ 조회 결과 record 를 팀 사용자 목록 projection 으로 조립한다. */
    public static TeamUserSummaryProjection from(Record record) {
        return new TeamUserSummaryProjection(
                record.get("is_leader", Boolean.class),
                record.get("user_id", Long.class),
                record.get("user_name", String.class),
                record.get("position_name", String.class),
                record.get("team_role", String.class)
        );
    }
}
