package com.ibank.axwms.domain.organization.evaluation.repository.jooq.query;

import com.ibank.axwms.domain.organization.evaluation.dto.GetUserEvaluationsApiDto;

/** service 가 visibility/기본 정렬/페이지 기본값을 정규화한 뒤 repository 에 전달하는 내부 query 다. */
public record UserEvaluationPageQuery(
        int page,
        int pageSize,
        Long evaluateeUserId,
        String sortDirection
) {

    /** API 요청 DTO 의 기본값을 보정해 repository 전용 query 로 변환한다. */
    public static UserEvaluationPageQuery of(Long evaluateeUserId, GetUserEvaluationsApiDto.Request request) {
        GetUserEvaluationsApiDto.Request query = request == null
                ? new GetUserEvaluationsApiDto.Request(null, null, null)
                : request;
        return new UserEvaluationPageQuery(
                query.pageOrDefault(),
                query.pageSizeOrDefault(),
                evaluateeUserId,
                query.sortDirectionOrDefault()
        );
    }

    /** API 는 1-indexed page 를 받지만 Spring Data PageRequest 는 0-indexed page 를 사용한다. */
    public int pageIndex() {
        return page - 1;
    }

    /** repository 정렬 분기에서 대소문자 입력 차이를 흡수한다. */
    public boolean ascending() {
        return "ASC".equalsIgnoreCase(sortDirection);
    }
}
