package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;

@Repository
@RequiredArgsConstructor
public class UserTeamJooqRepositoryImpl implements UserTeamJooqRepository {

    private static final String ACTIVE_USER_TEAM_STATUS = "ACTIVE";

    private final DSLContext dsl;

    @Override
    public List<UserTeamSummaryProjection> findUserTeamSummaries(Long userId) {
        return dsl.select(
                        TB_USER_TEAM.IS_PRIMARY,
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_USER_TEAM.IS_LEADER,
                        TB_USER_TEAM.TEAM_ROLE,
                        TB_USER_TEAM.ALLOCATION
                )
                .from(TB_USER_TEAM)
                .join(TB_TEAM).on(TB_TEAM.TEAM_ID.eq(TB_USER_TEAM.TEAM_ID))
                .where(TB_USER_TEAM.USER_ID.eq(userId))
                .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                .and(TB_TEAM.DELETED_AT.isNull())
                .orderBy(TB_USER_TEAM.IS_PRIMARY.desc(), TB_USER_TEAM.IS_LEADER.desc())
                .fetch(UserTeamSummaryProjection::from);
    }

}
