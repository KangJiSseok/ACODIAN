package com.ibank.axwms.domain.organization.team.repository.jooq;

import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamStatusSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamUserSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.global.jooq.tables.TbUser;
import com.ibank.axwms.global.jooq.tables.TbUserTeam;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM_ADMIN;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;

@Repository
@RequiredArgsConstructor
public class TeamJooqRepositoryImpl implements TeamJooqRepository {

    private static final String ACTIVE_TEAM_STATUS = "ACTIVE";
    private static final String INACTIVE_TEAM_STATUS = "INACTIVE";
    private static final String ACTIVE_USER_TEAM_STATUS = "ACTIVE";
    private static final String DEPT_HEAD_ROLE = "DEPT_HEAD";
    private static final String PRIMARY_ALLOCATION = "주담당";

    private final DSLContext dsl;

    /** 로그인 사용자가 볼 수 있는 팀 목록을 고정 정렬 정책에 따라 페이지로 조회한다. */
    @Override
    public Page<TeamSummaryProjection> findTeamPage(Long userId, TeamPageQuery query) {
        int pageIndex = query.pageIndex();
        int pageSize = query.pageSize();
        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

        Condition baseCondition = visibleTeamCondition(userId);
        long total = dsl.selectCount()
                .from(TB_TEAM)
                .where(baseCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }

        //현재 로그인 사용자가 이 팀에서 어떤 membership을 갖는지 조회
        TbUserTeam callerMembership = TB_USER_TEAM.as("caller_membership");
        //해당 팀의 ACTIVE 멤버 수를 세기 위한 JOIN
        TbUserTeam activeMembership = TB_USER_TEAM.as("active_membership");
        //해당 팀의 리더 membership을 찾기 위한 JOIN
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        //리더 membership의 user_id로 리더 이름을 가져오기 위한 JOIN
        TbUser leaderUser = TB_USER.as("leader_user");

        //팀 리더 user id
        Field<Long> teamLeaderId = leaderMembership.USER_ID.as("team_leader_id");
        //팀 리더 이름
        Field<String> teamLeaderName = leaderUser.USER_NAME.as("team_leader_name");
        //ACTIVE membership 수
        Field<Long> memberCount = DSL.count(activeMembership.USER_TEAM_ID).cast(Long.class).as("member_count");
        //현재 사용자가 이 팀 리더인지
        Field<Boolean> myIsLeaderValue = DSL.coalesce(callerMembership.IS_LEADER, false);
        Field<Boolean> myIsLeader = myIsLeaderValue.as("my_is_leader");
        //현재 사용자의 팀 내 역할
        Field<String> teamRole = callerMembership.TEAM_ROLE.as("team_role");
        //현재 사용자의 팀 배치 성격 (주담당, 겸임)
        Field<String> allocation = callerMembership.ALLOCATION.as("allocation");
        //현재 사용자의 주 담당 팀 소속 여부
        Field<Boolean> myIsPrimaryValue = DSL.coalesce(callerMembership.IS_PRIMARY, false);
        Field<Boolean> myIsPrimary = myIsPrimaryValue.as("is_primary");


        //정렬 조건
        Field<Integer> activeTeamPriority = DSL.when(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS), 0)
                .otherwise(1)
                .as("active_team_priority");
        Field<Integer> myLeaderPriority = DSL.when(myIsLeaderValue.isTrue(), 0)
                .otherwise(1)
                .as("my_leader_priority");
        Field<Integer> allocationPriority = DSL.when(
                callerMembership.ALLOCATION.eq(PRIMARY_ALLOCATION), 0)
                .otherwise(1)
                .as("allocation_priority");
        Field<Integer> primaryPriority = DSL.when(myIsPrimaryValue.isTrue(), 0)
                .otherwise(1)
                .as("primary_priority");


        //실제 SQL문
        List<TeamSummaryProjection> items = dsl.select(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DESCRIPTION,
                        teamLeaderId,
                        teamLeaderName,
                        memberCount,
                        myIsLeader,
                        teamRole,
                        allocation,
                        myIsPrimary,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE,
                        activeTeamPriority,
                        myLeaderPriority,
                        allocationPriority,
                        primaryPriority
                )
                .from(TB_TEAM)
                .leftJoin(callerMembership).on(callerMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(callerMembership.USER_ID.eq(userId))
                        .and(callerMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)))
                .leftJoin(activeMembership).on(activeMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(activeMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)))
                .leftJoin(leaderMembership).on(leaderMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(leaderMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                        .and(leaderMembership.IS_LEADER.isTrue()))
                .leftJoin(leaderUser).on(leaderUser.USER_ID.eq(leaderMembership.USER_ID))
                .where(baseCondition)
                .groupBy(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DESCRIPTION,
                        leaderMembership.USER_ID,
                        leaderUser.USER_NAME,
                        callerMembership.IS_LEADER,
                        callerMembership.TEAM_ROLE,
                        callerMembership.ALLOCATION,
                        callerMembership.IS_PRIMARY,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE
                )
                .orderBy(
                        activeTeamPriority.asc(),
                        myLeaderPriority.asc(),
                        allocationPriority.asc(),
                        primaryPriority.asc(),
                        TB_TEAM.TEAM_ID.asc()
                )
                .limit(pageSize)
                .offset((long) pageIndex * pageSize)
                .fetch(TeamSummaryProjection::from);

        return new PageImpl<>(items, pageRequest, total);
    }

    /** 로그인 사용자가 볼 수 있는 팀을 상태별로 집계한다. */
    @Override
    public TeamStatusSummaryProjection countTeamSummary(Long userId) {
        Field<Long> activeTeamCountValue = DSL.coalesce(
                        DSL.sum(DSL.when(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS), 1).otherwise(0)),
                        0)
                .cast(Long.class);
        Field<Long> inactiveTeamCountValue = DSL.coalesce(
                        DSL.sum(DSL.when(TB_TEAM.STATUS_CODE.eq(INACTIVE_TEAM_STATUS), 1).otherwise(0)),
                        0)
                .cast(Long.class);
        Field<Long> activeTeamCount = activeTeamCountValue.as("active_team_count");
        Field<Long> inactiveTeamCount = inactiveTeamCountValue.as("inactive_team_count");
        Field<Long> totalTeamCount = activeTeamCountValue.add(inactiveTeamCountValue).as("total_team_count");

        return dsl.select(activeTeamCount, inactiveTeamCount, totalTeamCount)
                .from(TB_TEAM)
                .where(visibleTeamCondition(userId))
                .fetchSingle(TeamStatusSummaryProjection::from);
    }

    /** 로그인 사용자가 볼 수 있는 단일 팀 상세와 DEPT_HEAD 팀 관리자 정보를 조회한다. */
    @Override
    public Optional<TeamDetailProjection> findTeamDetail(Long userId, Long teamId) {
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        TbUser leaderUser = TB_USER.as("leader_user");
        TbUser deptHeadAdminUser = TB_USER.as("dha_user");

        Field<Long> teamLeaderId = leaderMembership.USER_ID.as("team_leader_id");
        Field<String> teamLeaderName = leaderUser.USER_NAME.as("team_leader_name");
        Field<Long> deptHeadAdminUserId = deptHeadAdminUser.USER_ID.as("dept_head_admin_user_id");
        Field<String> deptHeadAdminUsername = deptHeadAdminUser.USER_NAME.as("dept_head_admin_username");

        return dsl.select(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DESCRIPTION,
                        teamLeaderId,
                        teamLeaderName,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE,
                        deptHeadAdminUserId,
                        deptHeadAdminUsername
                )
                .from(TB_TEAM)
                .leftJoin(leaderMembership).on(leaderMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(leaderMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                        .and(leaderMembership.IS_LEADER.isTrue()))
                .leftJoin(leaderUser).on(leaderUser.USER_ID.eq(leaderMembership.USER_ID))
                .leftJoin(deptHeadAdminUser).on(
                        deptHeadAdminUser.USER_ID.in(
                                DSL.select(TB_TEAM_ADMIN.USER_ID)
                                        .from(TB_TEAM_ADMIN)
                                        .where(TB_TEAM_ADMIN.TEAM_ID.eq(TB_TEAM.TEAM_ID)))
                        .and(deptHeadAdminUser.ROLE_CODE.eq(DEPT_HEAD_ROLE)))
                .where(TB_TEAM.TEAM_ID.eq(teamId))
                .and(visibleTeamCondition(userId))
                .fetchOptional(TeamDetailProjection::from);
    }

    /** 사용자가 볼 수 있는 팀의 ACTIVE 사용자 목록을 조회한다. 팀이 없거나 visible scope 밖이면 empty 를 반환한다. */
    @Override
    public Optional<List<TeamUserSummaryProjection>> findTeamUsers(Long userId, Long teamId) {
        TbUserTeam membership = TB_USER_TEAM.as("membership");
        TbUser user = TB_USER.as("team_user");

        Field<Boolean> isLeader = membership.IS_LEADER.as("is_leader");
        Field<Long> memberUserId = user.USER_ID.as("user_id");
        Field<String> userName = user.USER_NAME.as("user_name");
        Field<String> positionName = user.POSITION_NAME.as("position_name");
        Field<String> teamRole = membership.TEAM_ROLE.as("team_role");
        Field<Integer> leaderPriority = DSL.when(membership.IS_LEADER.isTrue(), 0)
                .otherwise(1)
                .as("leader_priority");

        // Result<Record6<Boolean, Long, String, String, String, Integer>> records 는 오히려 가독성 떨어진다.
        // jOOQ는 select에 넘긴 필드 개수와 타입을 제네릭으로 받는다.
        // JOOQ의 장점 : 컴파일러가 이미 타입을 알고있다.
        // 따라서 타입을 이미 알고있고, 제네릭이 복잡할때 var를 쓰면 좋다고 한다.
        var records = dsl.select(
                        isLeader,
                        memberUserId,
                        userName,
                        positionName,
                        teamRole,
                        leaderPriority
                )
                .from(TB_TEAM)
                .leftJoin(membership).on(membership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(membership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)))
                .leftJoin(user).on(user.USER_ID.eq(membership.USER_ID))
                .where(TB_TEAM.TEAM_ID.eq(teamId))
                .and(visibleTeamCondition(userId))
                .orderBy(leaderPriority.asc(), user.USER_ID.asc())
                .fetch();

        //결과가 없다면 팀이 없거나, 접근권한이 없다.
        if (records.isEmpty()) {
            return Optional.empty();
        }

        List<TeamUserSummaryProjection> users = records.stream()
                .filter(r -> r.get(memberUserId) != null)
                .map(TeamUserSummaryProjection::from)
                .toList();
        return Optional.of(users);
    }

    /** soft-delete 되지 않았고 호출자의 admin grant 또는 ACTIVE membership 에 포함된 팀만 통과시킨다. */
    private Condition visibleTeamCondition(Long userId) {
        return TB_TEAM.DELETED_AT.isNull()
                .and(TB_TEAM.TEAM_ID.in(visibleTeamIds(userId)));
    }

    /** admin grant 팀과 ACTIVE membership 팀을 DISTINCT 합집합으로 만든다. */
    private Select<Record1<Long>> visibleTeamIds(Long userId) {
        return DSL.select(TB_TEAM_ADMIN.TEAM_ID)
                .from(TB_TEAM_ADMIN)
                .where(TB_TEAM_ADMIN.USER_ID.eq(userId))
                .union(DSL.select(TB_USER_TEAM.TEAM_ID)
                        .from(TB_USER_TEAM)
                        .where(TB_USER_TEAM.USER_ID.eq(userId))
                        .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)));
    }
}
