package com.ibank.axwms.domain.worklog.repository.jooq.projection;

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
}
