package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import org.springframework.data.domain.Page;

public interface UserTeamJooqRepository {

    /** 단일 팀 상세의 사용자 탭에 필요한 membership 목록을 팀장 우선 정렬로 조회한다. */
    Page<TeamUserProjection> findTeamUserPage(Long teamId, TeamUsersQuery query);
}
