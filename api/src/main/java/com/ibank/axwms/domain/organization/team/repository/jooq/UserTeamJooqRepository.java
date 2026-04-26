package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import org.springframework.data.domain.Page;

public interface UserTeamJooqRepository {

    Page<TeamUserProjection> findTeamUserPage(Long teamId, GetTeamUsersApiDto.Request request);
}
