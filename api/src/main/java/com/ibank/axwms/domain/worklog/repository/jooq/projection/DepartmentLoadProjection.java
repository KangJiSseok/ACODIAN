package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/** 부서별 활성(미완료) worklog 수. 부서 부하 편중 지수와 그래프 위젯의 입력. */
public record DepartmentLoadProjection(
        Long departmentId,
        String departmentName,
        int activeWorklogCount
) {
}
