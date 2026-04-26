package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface TeamJooqRepository {

    Page<TeamListProjection> findTeamPage(TeamPageQuery query);

    TeamSummaryProjection findTeamSummary(TeamSummaryQuery query);

    Optional<TeamDetailProjection> findTeamDetail(Long teamId);

    Page<TeamWorklogProjection> findTeamWorklogPage(Long teamId, TeamWorklogsQuery query);
}
