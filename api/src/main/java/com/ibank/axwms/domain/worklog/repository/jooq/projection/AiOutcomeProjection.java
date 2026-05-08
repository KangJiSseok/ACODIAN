package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/**
 * AI 처리 결과 집계 — COMPLETED 와 FAILED 의 카운트. service / DTO 가 success rate 를 계산한다.
 *
 * <p>현재는 {@link DashboardScopeSummaryProjection#aiOutcome()} 가 통합 SELECT 결과에서 분해해 만든다.
 * 별도 SELECT 로 직접 만들 필요가 생기면 jOOQ {@code Record} 기반 정적 팩토리를 추가하면 된다.
 */
public record AiOutcomeProjection(
        int success,
        int failed
) {
}
