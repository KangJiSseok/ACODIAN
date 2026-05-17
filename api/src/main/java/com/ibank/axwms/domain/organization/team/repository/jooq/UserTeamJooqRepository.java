package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import java.util.List;

public interface UserTeamJooqRepository {

    /** 사용자의 ACTIVE membership 중 soft-delete 되지 않은 팀을 주 소속·리더 순으로 조회한다. */
    List<UserTeamSummaryProjection> findUserTeamSummaries(Long userId);

    /** 현재 사용자 프로필 문맥에서 ACTIVE membership 이면서 soft-delete 되지 않은 ACTIVE 팀만 주 소속·리더 순으로 조회한다. */
    List<UserTeamSummaryProjection> findActiveUserTeamSummaries(Long userId);

}
