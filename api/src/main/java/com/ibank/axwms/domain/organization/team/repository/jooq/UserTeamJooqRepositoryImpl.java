package com.ibank.axwms.domain.organization.team.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamUsersQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserTeamJooqRepositoryImpl implements UserTeamJooqRepository {

    private final DSLContext dsl;
    private final TeamMembershipConditionSupport teamMembershipConditionSupport;

    /**
     * 단일 팀의 활성 membership 목록을 조회한다.
     * 팀장 우선 정렬과 email 노출 규칙을 한 번에 보장하기 위해 user 테이블을 함께 조인한다.
     */
    @Override
    public Page<TeamUserProjection> findTeamUserPage(Long teamId, TeamUsersQuery query) {
        org.jooq.Condition scopeCondition = TB_USER_TEAM.TEAM_ID.eq(teamId)
                .and(teamMembershipConditionSupport.activeMembership(TB_USER_TEAM.STATUS_CODE));
        long totalCount = dsl.selectCount()
                .from(TB_USER_TEAM)
                .where(scopeCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        List<TeamUserProjection> items = dsl.select(
                        TB_USER_TEAM.TEAM_LEADER,
                        TB_USER.USER_ID,
                        TB_USER.USER_NAME,
                        TB_USER.POSITION_NAME,
                        TB_USER.EMAIL,
                        TB_USER_TEAM.TEAM_ROLE,
                        TB_USER_TEAM.ALLOCATION
                )
                .from(TB_USER_TEAM)
                .join(TB_USER).on(TB_USER.USER_ID.eq(TB_USER_TEAM.USER_ID))
                .where(scopeCondition)
                .orderBy(TB_USER_TEAM.TEAM_LEADER.desc(), TB_USER.USER_ID.asc())
                .limit(query.pageSize())
                .offset((query.page() - 1) * query.pageSize())
                .fetch(record -> new TeamUserProjection(
                        record.get(TB_USER_TEAM.TEAM_LEADER),
                        record.get(TB_USER.USER_ID),
                        record.get(TB_USER.USER_NAME),
                        record.get(TB_USER.POSITION_NAME),
                        record.get(TB_USER.EMAIL),
                        record.get(TB_USER_TEAM.TEAM_ROLE),
                        record.get(TB_USER_TEAM.ALLOCATION)
                ));

        return new PageImpl<>(items, PageRequest.of(query.page() - 1, query.pageSize()), totalCount);
    }
}
