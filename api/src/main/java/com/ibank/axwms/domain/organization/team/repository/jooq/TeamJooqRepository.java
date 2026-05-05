package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamMemberFilterProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface TeamJooqRepository {

    /** 사용자에게 visible 한 팀 목록을 spec 고정 정렬과 pagination 으로 조회한다. */
    Page<TeamSummaryProjection> findTeamPage(Long userId, TeamPageQuery query);

    /** 사용자에게 visible 한 팀의 상태별 개수를 조회한다. */
    TeamStatusSummaryProjection countTeamSummary(Long userId);

    /** 사용자가 볼 수 있는 단일 팀 상세를 조회한다. */
    Optional<TeamDetailProjection> findTeamDetail(Long userId, Long teamId);

    /** 사용자가 볼 수 있는 팀의 ACTIVE 사용자 목록을 리더 우선, 사용자 ID 순으로 조회한다. */
    Optional<List<TeamUserSummaryProjection>> findTeamUsers(Long userId, Long teamId);

    /**
     * 사용자가 볼 수 있는 팀과 각 팀의 ACTIVE 멤버를 한 번의 LEFT JOIN 쿼리로 조회한다.
     * 멤버가 없는 팀은 userId/userName 이 null 인 행으로 들어오며, service 가 teamId 로 그룹핑한다.
     */
    List<TeamMemberFilterProjection> findVisibleTeamsWithMembersForFilter(Long userId);
}
