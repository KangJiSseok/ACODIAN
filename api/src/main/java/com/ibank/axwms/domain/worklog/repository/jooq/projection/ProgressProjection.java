package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/**
 * 단일 진행 현황 (완료 / 전체) 카운트. service / DTO 가 rate 를 계산한다.
 * 전사 / 부서 / 팀 등 scope 의 단일 진행률 집계에 공통 사용된다.
 *
 * <p>현재는 {@link DashboardScopeSummaryProjection#progress()} 가 통합 SELECT 결과에서 분해해 만든다.
 * 별도 SELECT 로 직접 만들 필요가 생기면 jOOQ {@code Record} 기반 정적 팩토리를 추가하면 된다.
 */
public record ProgressProjection(
        int completed,
        int total
) {
}
