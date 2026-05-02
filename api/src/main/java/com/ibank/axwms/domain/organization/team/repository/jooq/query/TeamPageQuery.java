package com.ibank.axwms.domain.organization.team.repository.jooq.query;

import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;

/** 팀 목록 repository 조회에 필요한 pagination 값을 정규화한 query 이다. */
public record TeamPageQuery(
        int page,
        int pageSize
) {

    /** API 요청 DTO 의 기본값을 보정해 repository 전용 query 로 변환한다. */
    public static TeamPageQuery from(GetTeamsApiDto.Request request) {
        GetTeamsApiDto.Request query = request == null ? new GetTeamsApiDto.Request(null, null) : request;
        return new TeamPageQuery(query.pageOrDefault(), query.pageSizeOrDefault());
    }

    public int pageIndex() {
        return page - 1;
    }
}
