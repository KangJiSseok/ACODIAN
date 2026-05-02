package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import java.time.LocalDate;
import org.jooq.Record;

/** 팀 목록 한 행에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record TeamSummaryProjection(
        Long teamId,
        String teamName,
        String statusCode,
        String description,
        Long teamLeaderId,
        String teamLeaderName,
        Long memberCount,
        Boolean myIsLeader,
        String teamRole,
        String allocation,
        Boolean isPrimary,
        LocalDate startDate,
        LocalDate expectedEndDate
) {

    /** JOOQ 조회 결과 record 를 팀 목록 projection 으로 조립한다. */
    public static TeamSummaryProjection from(Record record) {
        return new TeamSummaryProjection(
                record.get("team_id", Long.class),
                record.get("team_name", String.class),
                record.get("status_code", String.class),
                record.get("description", String.class),
                record.get("team_leader_id", Long.class),
                record.get("team_leader_name", String.class),
                record.get("member_count", Long.class),
                record.get("my_is_leader", Boolean.class),
                record.get("team_role", String.class),
                record.get("allocation", String.class),
                record.get("is_primary", Boolean.class),
                record.get("start_date", LocalDate.class),
                record.get("expected_end_date", LocalDate.class)
        );
    }
}
