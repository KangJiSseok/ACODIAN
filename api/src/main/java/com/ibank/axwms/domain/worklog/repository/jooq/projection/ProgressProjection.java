package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/**
 * 단일 진행 현황 (완료 / 전체) 카운트. service 가 rate 를 계산한다.
 * 전사 / 부서 / 팀 등 scope 의 단일 진행률 집계에 공통 사용된다.
 */
public record ProgressProjection(
        int completed,
        int total
) {
}
