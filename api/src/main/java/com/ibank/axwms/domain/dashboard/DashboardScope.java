package com.ibank.axwms.domain.dashboard;

/**
 * Dashboard 조회 범위. URL 의 scope 파라미터로 받아 응답 다형성의 디스크리미네이터로 사용한다.
 */
public enum DashboardScope {
    /** 로그인 사용자의 개인 대시보드. 모든 role 이 접근 가능. */
    ME,
    /** 전체 부서 비교 대시보드. DIRECTOR 만 접근 가능. */
    DEPARTMENT_COMPARISON,
    /** 단일 부서 상세 대시보드. DIRECTOR 또는 자기 부서를 관리하는 DEPT_HEAD. */
    DEPARTMENT_DETAIL,
    /** 단일 팀 상세 대시보드. DIRECTOR 또는 팀이 속한 부서의 DEPT_HEAD 또는 팀의 TEAM_LEAD. */
    TEAM_DETAIL
}
