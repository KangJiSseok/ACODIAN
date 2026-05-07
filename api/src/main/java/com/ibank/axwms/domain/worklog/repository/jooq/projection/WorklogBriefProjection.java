package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;

import org.jooq.Record;

import java.time.LocalDate;

/**
 * 대시보드/목록 위젯에서 worklog 한 행을 가볍게 표현하는 공통 projection.
 * scope 에 따라 author/team/department 필드는 선택적으로 채워진다.
 */
public record WorklogBriefProjection(
        Long worklogId,
        String title,
        String statusCode,
        LocalDate dueDate,
        Long authorId,
        String authorName,
        Long teamId,
        String teamName,
        Long departmentId,
        String departmentName
) {

    /**
     * worklog 4개 컬럼만 있는 단순 행 (ME 위젯용).
     * author/team/department 6개 필드는 명시적으로 null — DTO 의 "부서/전사 위젯에서만 채움" 정책을
     * projection 단계에서 강제한다. ME 쿼리에 user/team/department join 을 추가해도 이 팩토리를 쓰는 한
     * 응답으로 새어나가지 않는다.
     */
    public static WorklogBriefProjection from(Record record) {
        return new WorklogBriefProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.DUE_DATE),
                null, null, null, null, null, null
        );
    }

    /** worklog + 작성자 + 팀 + 부서 join 한 행 (DEPARTMENT_COMPARISON 위젯용). */
    public static WorklogBriefProjection fromOrg(Record record) {
        return new WorklogBriefProjection(
                record.get(TB_WORKLOG.WORKLOG_ID),
                record.get(TB_WORKLOG.TITLE),
                record.get(TB_WORKLOG.STATUS_CODE),
                record.get(TB_WORKLOG.DUE_DATE),
                record.get(TB_WORKLOG.AUTHOR_ID),
                record.get(TB_USER.USER_NAME),
                record.get(TB_WORKLOG.TEAM_ID),
                record.get(TB_TEAM.TEAM_NAME),
                record.get(TB_DEPARTMENT.DEPARTMENT_ID),
                record.get(TB_DEPARTMENT.DEPARTMENT_NAME)
        );
    }
}
