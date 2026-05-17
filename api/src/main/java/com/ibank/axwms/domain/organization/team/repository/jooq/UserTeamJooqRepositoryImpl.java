package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.UserTeamSummaryProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static org.jooq.impl.DSL.trueCondition;

@Repository
@RequiredArgsConstructor
public class UserTeamJooqRepositoryImpl implements UserTeamJooqRepository {

    private static final String ACTIVE_USER_TEAM_STATUS = "ACTIVE";
    private static final String ACTIVE_TEAM_STATUS = "ACTIVE";

    private final DSLContext dsl;

    /** 기존 사용자 상세 조회 계약에 맞춰 ACTIVE membership 과 soft-delete 제외 조건만 적용한다. */
    @Override
    public List<UserTeamSummaryProjection> findUserTeamSummaries(Long userId) {
        return fetchUserTeamSummaries(userId, trueCondition());
    }

    /** 현재 사용자 프로필의 팀 컨텍스트에는 운영 중인 ACTIVE 팀만 포함한다. */
    @Override
    public List<UserTeamSummaryProjection> findActiveUserTeamSummaries(Long userId) {
        return fetchUserTeamSummaries(userId, TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS));
    }

    /** 조회 계약별 추가 팀 상태 조건을 주입하면서 공통 projection/정렬 계약을 한 곳에서 유지한다. */
    private List<UserTeamSummaryProjection> fetchUserTeamSummaries(Long userId, Condition additionalTeamCondition) {
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
                .and(additionalTeamCondition)
                .orderBy(TB_USER_TEAM.IS_PRIMARY.desc(), TB_USER_TEAM.IS_LEADER.desc())
                .fetch(UserTeamSummaryProjection::from);
    }

}
