package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;

/** 부서별 활성(미완료) worklog 수. 부서 부하 편중 지수와 그래프 위젯의 입력. */
public record DepartmentLoadProjection(
        Long departmentId,
        String departmentName,
        int activeWorklogCount
) {

    /**
     * activeWorklogCount 는 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static DepartmentLoadProjection from(Record record, Field<Integer> activeWorklogCount) {
        return new DepartmentLoadProjection(
                record.get(TB_DEPARTMENT.DEPARTMENT_ID),
                record.get(TB_DEPARTMENT.DEPARTMENT_NAME),
                record.get(activeWorklogCount)
        );
    }
}
