package com.ibank.axwms.domain.organization.evaluation.repository.jooq.query;

/** service 가 visibility/기본 정렬/페이지 기본값을 정규화한 뒤 repository 에 전달하는 내부 query 다. */
public record UserEvaluationPageQuery(
        int page,
        int pageSize,
        Long evaluateeUserId,
        String sortDirection
) {
}
