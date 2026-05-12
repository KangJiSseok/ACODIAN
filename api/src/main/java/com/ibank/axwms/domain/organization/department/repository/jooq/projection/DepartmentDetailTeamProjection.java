package com.ibank.axwms.domain.organization.department.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;

import com.ibank.axwms.global.jooq.tables.TbUser;
import java.time.LocalDate;
import org.jooq.Field;
import org.jooq.Record;

/** 부서 상세 teams[] 는 nullable department ownership 기준으로 선별된 팀 행만 담는다. */
public record DepartmentDetailTeamProjection(
        Long teamId,
        String teamName,
        Long leaderId,
        String leaderName,
        LocalDate startDate,
        LocalDate expectedEndDate,
        long memberCount
) {

    /** repository 가 만든 집계 Field 와 리더 alias 를 명시해 team row 조립 기준을 projection 옆에 둔다. */
    public static DepartmentDetailTeamProjection from(Record record,
                                                      Field<Long> leaderId,
                                                      TbUser leaderUser,
                                                      Field<Long> memberCount) {
        return new DepartmentDetailTeamProjection(
                record.get(TB_TEAM.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(leaderId),
                record.get(leaderUser.USER_NAME),
                record.get(TB_TEAM.START_DATE),
                record.get(TB_TEAM.EXPECTED_END_DATE),
                record.get(memberCount)
        );
    }
}
