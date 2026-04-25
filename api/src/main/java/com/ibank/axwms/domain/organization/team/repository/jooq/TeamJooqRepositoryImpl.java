package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.dto.GetTeamWorklogsApiDto;
import com.ibank.axwms.domain.organization.team.dto.GetTeamsApiDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class TeamJooqRepositoryImpl implements TeamJooqRepository {

    @Override
    public Page<TeamListProjection> findTeamPage(GetTeamsApiDto.Request request) {
        return new PageImpl<>(List.of(), PageRequest.of(request.pageOrDefault() - 1, request.pageSizeOrDefault()), 0);
    }

    @Override
    public Optional<TeamDetailProjection> findTeamDetail(Long teamId) {
        return Optional.empty();
    }

    @Override
    public Page<TeamWorklogProjection> findTeamWorklogPage(Long teamId, GetTeamWorklogsApiDto.Request request) {
        return new PageImpl<>(List.of(), PageRequest.of(request.pageOrDefault() - 1, request.pageSizeOrDefault()), 0);
    }
}
