package com.ibank.axwms.domain.worklog.repository.jooq.projection;

/** AI 처리 결과 집계 — COMPLETED 와 FAILED 의 카운트. service 가 success rate 를 계산한다. */
public record AiOutcomeProjection(
        int success,
        int failed
) {
}
