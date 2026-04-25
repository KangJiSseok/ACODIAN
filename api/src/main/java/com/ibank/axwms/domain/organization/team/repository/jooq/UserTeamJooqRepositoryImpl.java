package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.dto.GetTeamUsersApiDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class UserTeamJooqRepositoryImpl implements UserTeamJooqRepository {

    @Override
    public Page<TeamUserProjection> findTeamUserPage(Long teamId, GetTeamUsersApiDto.Request request) {
        return new PageImpl<>(List.of(), PageRequest.of(request.pageOrDefault() - 1, request.pageSizeOrDefault()), 0);
    }
}
