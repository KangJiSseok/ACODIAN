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

    /** 최신 team spec 기준 목록 화면에 필요한 팀 페이지를 조회한다. */
    Page<TeamListProjection> findTeamPage(TeamPageQuery query);

    /** 목록과 분리된 상단 KPI 영역을 위해 visible scope 기준 팀 집계를 조회한다. */
    TeamSummaryProjection findTeamSummary(TeamSummaryQuery query);

    /** 단일 팀 상세 화면에 필요한 기본 정보와 업무일지 집계를 함께 조회한다. */
    Optional<TeamDetailProjection> findTeamDetail(Long teamId);

    /** 단일 팀 상세의 업무일지 탭에 필요한 페이지를 상태 우선순위 기준으로 조회한다. */
    Page<TeamWorklogProjection> findTeamWorklogPage(Long teamId, TeamWorklogsQuery query);
}
