package com.ibank.axwms.domain.worklog.repository.jooq.projection;

import org.jooq.Field;
import org.jooq.Record;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;

/**
 * 부서별 진행 현황 한 행. 부서별 (완료, 전체) worklog 카운트를 담는다.
 * service 가 completion rate 를 계산해 응답으로 변환한다.
 */
public record DepartmentProgressProjection(
        Long departmentId,
        String departmentName,
        int completed,
        int total
) {

    /**
     * completed/total 은 repository 쿼리에서 만든 계산 Field 라 record 외 추가 인자로 받는다.
     * 같은 Field 인스턴스를 select(...) 와 여기 from(record, ...) 에 함께 전달해야 record.get(field) 가 매칭된다.
     */
    public static DepartmentProgressProjection from(Record record,
                                                    Field<Integer> completed,
                                                    Field<Integer> total) {
        return new DepartmentProgressProjection(
                record.get(TB_DEPARTMENT.DEPARTMENT_ID),
                record.get(TB_DEPARTMENT.DEPARTMENT_NAME),
                record.get(completed),
                record.get(total)
        );
    }
}
