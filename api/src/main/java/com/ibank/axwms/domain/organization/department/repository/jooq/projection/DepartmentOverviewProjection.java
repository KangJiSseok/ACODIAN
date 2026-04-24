package com.ibank.axwms.domain.organization.department.repository.jooq.projection;

/** 부서 목록 화면 상단 KPI 집계를 표현하는 읽기 전용 projection 이다. */
public record DepartmentOverviewProjection(
        long activeDepartmentCount,
        long activeTeamCount,
        long activeUserCount
) {
}
