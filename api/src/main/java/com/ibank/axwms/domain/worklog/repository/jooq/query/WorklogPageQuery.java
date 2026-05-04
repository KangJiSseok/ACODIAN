package com.ibank.axwms.domain.worklog.repository.jooq.query;

import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;

public record WorklogPageQuery(
    int page,
    int pageSize
) {
    public static WorklogPageQuery from(GetWorklogsApiDto.Request request) {
        return new WorklogPageQuery(request.pageOrDefault(), request.pageSizeOrDefault());
    }

    public int pageIndex() { return page - 1; }
}

