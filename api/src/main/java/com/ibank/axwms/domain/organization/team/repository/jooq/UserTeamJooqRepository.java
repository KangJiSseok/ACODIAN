package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import org.springframework.data.domain.Page;

public interface UserTeamJooqRepository {

    Page<TeamUserProjection> findTeamUserPage(Long teamId, TeamUsersQuery query);
}
