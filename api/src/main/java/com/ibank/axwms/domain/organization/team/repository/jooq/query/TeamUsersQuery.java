package com.ibank.axwms.domain.organization.team.repository.jooq.query;

/** 팀 사용자 탭 조회에 필요한 페이지네이션 입력만 담는 내부 query 다. */
public record TeamUsersQuery(
        int page,
        int pageSize
) {
}
