package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;

/**
 * 팀원별 활성(미완료) worklog 수. 팀 부하 편중 지수 (Gini) 와 그래프 위젯의 입력.
 * TEAM_DETAIL 위젯에서 ACTIVE 멤버 baseline LEFT JOIN 으로 조회 — 활성 worklog 0 건인 멤버도 포함.
 */
public record MemberLoadProjection(
        Long userId,
        String userName,
        int activeWorklogCount
) {

    /**
     * activeWorklogCount 는 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static MemberLoadProjection from(Record record, Field<Integer> activeWorklogCount) {
        return new MemberLoadProjection(
                record.get(TB_USER.USER_ID),
                record.get(TB_USER.USER_NAME),
                record.get(activeWorklogCount)
        );
    }
}
