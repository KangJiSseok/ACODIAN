package com.ibank.axwms.domain.organization.team.repository.jooq.query;

/** 팀 목록 조회에서 service 가 ownership/기본값을 정규화한 뒤 repository 에 전달하는 내부 query 다. */
public record TeamPageQuery(
        int page,
        int pageSize,
        Long departmentId,
        Long visibleTeamId,
        Long principalUserId
) {
}
