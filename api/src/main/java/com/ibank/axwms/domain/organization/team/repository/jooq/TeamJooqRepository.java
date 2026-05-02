package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import org.springframework.data.domain.Page;

public interface TeamJooqRepository {

    /** 사용자에게 visible 한 팀 목록을 spec 고정 정렬과 pagination 으로 조회한다. */
    Page<TeamSummaryProjection> findTeamPage(Long userId, TeamPageQuery query);
}
