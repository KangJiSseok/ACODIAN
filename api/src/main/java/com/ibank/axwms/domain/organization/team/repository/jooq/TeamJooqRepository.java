package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface TeamJooqRepository {

    Page<TeamListProjection> findTeamPage(GetTeamsApiDto.Request request);

    Optional<TeamDetailProjection> findTeamDetail(Long teamId);

    Page<TeamWorklogProjection> findTeamWorklogPage(Long teamId, GetTeamWorklogsApiDto.Request request);
}
