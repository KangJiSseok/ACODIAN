package com.ibank.axwms.domain.organization.team.repository.jooq.projection;

import java.time.LocalDate;
import org.jooq.Record;

/** 팀 상세 조회에 필요한 필드만 담는 읽기 전용 projection 이다. */
public record TeamDetailProjection(
        Long teamId,
        String teamName,
        String statusCode,
        String description,
        Long teamLeaderId,
        String teamLeaderName,
        LocalDate startDate,
        LocalDate expectedEndDate,
        Long deptHeadAdminUserId,
        String deptHeadAdminUsername
) {

    /** JOOQ 조회 결과 record 를 팀 상세 projection 으로 조립한다. */
    public static TeamDetailProjection from(Record record) {
        return new TeamDetailProjection(
                record.get("team_id", Long.class),
                record.get("team_name", String.class),
                record.get("status_code", String.class),
                record.get("description", String.class),
                record.get("team_leader_id", Long.class),
                record.get("team_leader_name", String.class),
                record.get("start_date", LocalDate.class),
                record.get("expected_end_date", LocalDate.class),
                record.get("dept_head_admin_user_id", Long.class),
                record.get("dept_head_admin_username", String.class)
        );
    }
}
