package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface TeamJooqRepository {

    /** 사용자에게 visible 한 팀 목록을 spec 고정 정렬과 pagination 으로 조회한다. */
    Page<TeamSummaryProjection> findTeamPage(Long userId, TeamPageQuery query);

    /** 사용자가 볼 수 있는 단일 팀 상세를 조회한다. */
    Optional<TeamDetailProjection> findTeamDetail(Long userId, Long teamId);
}
