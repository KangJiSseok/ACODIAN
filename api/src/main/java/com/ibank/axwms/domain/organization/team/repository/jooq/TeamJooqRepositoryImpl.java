package com.ibank.axwms.domain.organization.team.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.upper;
import static org.jooq.impl.DSL.when;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.global.jooq.tables.TbDepartment;
import com.ibank.axwms.global.jooq.tables.TbTeam;
import com.ibank.axwms.global.jooq.tables.TbUser;
import com.ibank.axwms.global.jooq.tables.TbUserTeam;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record17;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamJooqRepositoryImpl implements TeamJooqRepository {

    private static final String ACTIVE_TEAM_STATUS = TeamStatus.ACTIVE.name();
    private static final String COMPLETED_WORKLOG_STATUS = WorklogStatus.COMPLETED.name();
    private static final String ACTIVE_EMPLOYMENT_STATUS = EmploymentStatus.ACTIVE.name();

    private final DSLContext dsl;
    private final TeamMembershipConditionSupport teamMembershipConditionSupport;

    @Override
    public Page<TeamListProjection> findTeamPage(TeamPageQuery query) {
        TbDepartment department = TB_DEPARTMENT.as("department");
        TbUser departmentHead = TB_USER.as("department_head");
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        TbUser leaderUser = TB_USER.as("leader_user");
        TbUserTeam activeMembership = TB_USER_TEAM.as("active_membership");
        TbUserTeam selfMembership = TB_USER_TEAM.as("self_membership");

        Field<Integer> memberCount = countDistinct(activeMembership.USER_ID).cast(Integer.class).as("member_count");
        Field<Boolean> myTeamLeader = coalesce(selfMembership.TEAM_LEADER, inline(false)).as("my_team_leader");
        Field<String> myAllocation = selfMembership.ALLOCATION.as("my_allocation");
        Field<Boolean> myPrimary = coalesce(selfMembership.IS_PRIMARY, inline(false)).as("my_primary");

        Condition scopeCondition = teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId());
        long totalCount = dsl.selectCount()
                .from(TB_TEAM)
                .where(scopeCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        List<TeamListProjection> items = dsl.select(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DEPARTMENT_ID,
                        department.DEPARTMENT_NAME,
                        TB_TEAM.DESCRIPTION,
                        department.DEPARTMENT_HEAD_USER_ID,
                        departmentHead.USER_NAME,
                        leaderUser.USER_ID,
                        leaderUser.USER_NAME,
                        memberCount,
                        myTeamLeader,
                        selfMembership.TEAM_ROLE,
                        myAllocation,
                        myPrimary,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE
                )
                .from(TB_TEAM)
                .join(department).on(TB_TEAM.DEPARTMENT_ID.eq(department.DEPARTMENT_ID))
                .leftJoin(departmentHead).on(department.DEPARTMENT_HEAD_USER_ID.eq(departmentHead.USER_ID))
                .leftJoin(leaderMembership).on(leaderMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(leaderMembership.STATUS_CODE))
                        .and(leaderMembership.TEAM_LEADER.isTrue()))
                .leftJoin(leaderUser).on(leaderMembership.USER_ID.eq(leaderUser.USER_ID))
                .leftJoin(activeMembership).on(activeMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(activeMembership.STATUS_CODE)))
                .leftJoin(selfMembership).on(selfMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(selfMembership.USER_ID.eq(query.principalUserId()))
                        .and(teamMembershipConditionSupport.activeMembership(selfMembership.STATUS_CODE)))
                .where(scopeCondition)
                .groupBy(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DEPARTMENT_ID,
                        department.DEPARTMENT_NAME,
                        TB_TEAM.DESCRIPTION,
                        department.DEPARTMENT_HEAD_USER_ID,
                        departmentHead.USER_NAME,
                        leaderUser.USER_ID,
                        leaderUser.USER_NAME,
                        selfMembership.TEAM_LEADER,
                        selfMembership.TEAM_ROLE,
                        selfMembership.ALLOCATION,
                        selfMembership.IS_PRIMARY,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE
                )
                .orderBy(
                        TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS).desc(),
                        coalesce(selfMembership.TEAM_LEADER, inline(false)).desc(),
                        allocationPriority(selfMembership.ALLOCATION).desc(),
                        coalesce(selfMembership.IS_PRIMARY, inline(false)).desc(),
                        TB_TEAM.TEAM_ID.asc()
                )
                .limit(query.pageSize())
                .offset((query.page() - 1) * query.pageSize())
                .fetch(this::toTeamListProjection);

        return new PageImpl<>(items, PageRequest.of(query.page() - 1, query.pageSize()), totalCount);
    }

    @Override
    public TeamSummaryProjection findTeamSummary(TeamSummaryQuery query) {
        return new TeamSummaryProjection(
                fetchActiveTeamCount(query),
                fetchTotalTeamCount(query),
                fetchActiveUserCount(query),
                fetchActiveTeamUserCount(query),
                fetchAllTeamUserCount(query)
        );
    }

    @Override
    public Optional<TeamDetailProjection> findTeamDetail(Long teamId) {
        TbDepartment department = TB_DEPARTMENT.as("department");
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        TbUser leaderUser = TB_USER.as("leader_user");

        Field<Integer> totalWorklogCount = dsl.selectCount()
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse()))
                .asField("total_worklog_count");
        Field<Integer> completedWorklogCount = dsl.selectCount()
                .from(TB_WORKLOG)
                .where(TB_WORKLOG.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(TB_WORKLOG.IS_DELETED.isFalse())
                        .and(TB_WORKLOG.STATUS_CODE.eq(COMPLETED_WORKLOG_STATUS)))
                .asField("completed_worklog_count");

        return dsl.select(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        TB_TEAM.STATUS_CODE,
                        TB_TEAM.DESCRIPTION,
                        TB_TEAM.DEPARTMENT_ID,
                        department.DEPARTMENT_NAME,
                        leaderUser.USER_ID,
                        leaderUser.USER_NAME,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE,
                        totalWorklogCount,
                        completedWorklogCount
                )
                .from(TB_TEAM)
                .join(department).on(TB_TEAM.DEPARTMENT_ID.eq(department.DEPARTMENT_ID))
                .leftJoin(leaderMembership).on(leaderMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(leaderMembership.STATUS_CODE))
                        .and(leaderMembership.TEAM_LEADER.isTrue()))
                .leftJoin(leaderUser).on(leaderMembership.USER_ID.eq(leaderUser.USER_ID))
                .where(teamScopeCondition(TB_TEAM, null, teamId))
                .fetchOptional(record -> new TeamDetailProjection(
                        record.get(TB_TEAM.TEAM_ID),
                        record.get(TB_TEAM.TEAM_NAME),
                        TeamStatus.valueOf(record.get(TB_TEAM.STATUS_CODE)),
                        record.get(TB_TEAM.DESCRIPTION),
                        record.get(TB_TEAM.DEPARTMENT_ID),
                        record.get(department.DEPARTMENT_NAME),
                        record.get(leaderUser.USER_ID),
                        record.get(leaderUser.USER_NAME),
                        record.get(TB_TEAM.START_DATE),
                        record.get(TB_TEAM.EXPECTED_END_DATE),
                        record.get(totalWorklogCount).longValue(),
                        record.get(completedWorklogCount).longValue()
                ));
    }

    @Override
    public Page<TeamWorklogProjection> findTeamWorklogPage(Long teamId, TeamWorklogsQuery query) {
        Condition scopeCondition = TB_WORKLOG.TEAM_ID.eq(teamId)
                .and(TB_WORKLOG.IS_DELETED.isFalse());
        long totalCount = dsl.selectCount()
                .from(TB_WORKLOG)
                .where(scopeCondition)
                .fetchSingle(0, Integer.class)
                .longValue();

        List<TeamWorklogProjection> items = dsl.select(
                        TB_WORKLOG.WORKLOG_ID,
                        TB_WORKLOG.TITLE,
                        TB_WORKLOG.REQUEST_CONTENT,
                        TB_WORKLOG.WORK_CONTENT,
                        TB_WORKLOG.AI_SUMMARY,
                        TB_WORKLOG.STATUS_CODE,
                        TB_WORKLOG.IMPORTANCE_CODE
                )
                .from(TB_WORKLOG)
                .where(scopeCondition)
                .orderBy(worklogStatusPriority(TB_WORKLOG.STATUS_CODE).asc(), TB_WORKLOG.WORKLOG_ID.desc())
                .limit(query.pageSize())
                .offset((query.page() - 1) * query.pageSize())
                .fetch(record -> new TeamWorklogProjection(
                        record.get(TB_WORKLOG.WORKLOG_ID),
                        record.get(TB_WORKLOG.TITLE),
                        record.get(TB_WORKLOG.REQUEST_CONTENT),
                        record.get(TB_WORKLOG.WORK_CONTENT),
                        record.get(TB_WORKLOG.AI_SUMMARY),
                        record.get(TB_WORKLOG.STATUS_CODE),
                        record.get(TB_WORKLOG.IMPORTANCE_CODE)
                ));

        return new PageImpl<>(items, PageRequest.of(query.page() - 1, query.pageSize()), totalCount);
    }

    private TeamListProjection toTeamListProjection(Record17<Long, String, String, Long, String, String, Long, String, Long, String, Integer, Boolean, String, String, Boolean, java.time.LocalDate, java.time.LocalDate> record) {
        return new TeamListProjection(
                record.value1(),
                record.value2(),
                TeamStatus.valueOf(record.value3()),
                record.value4(),
                record.value5(),
                record.value6(),
                record.value7(),
                record.value8(),
                record.value9(),
                record.value10(),
                record.value11(),
                record.value12(),
                record.value13(),
                record.value14(),
                record.value15(),
                record.value16(),
                record.value17()
        );
    }

    private long fetchActiveTeamCount(TeamSummaryQuery query) {
        return dsl.selectCount()
                .from(TB_TEAM)
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId())
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    private long fetchTotalTeamCount(TeamSummaryQuery query) {
        return dsl.selectCount()
                .from(TB_TEAM)
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId()))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    private long fetchActiveUserCount(TeamSummaryQuery query) {
        return dsl.select(countDistinct(TB_USER.USER_ID))
                .from(TB_TEAM)
                .join(TB_USER_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(TB_USER_TEAM.STATUS_CODE)))
                .join(TB_USER).on(TB_USER.USER_ID.eq(TB_USER_TEAM.USER_ID))
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId())
                        .and(TB_USER.EMPLOYMENT_STATUS.eq(ACTIVE_EMPLOYMENT_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    private long fetchActiveTeamUserCount(TeamSummaryQuery query) {
        return dsl.select(countDistinct(TB_USER_TEAM.USER_ID))
                .from(TB_TEAM)
                .join(TB_USER_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(TB_USER_TEAM.STATUS_CODE)))
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId())
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    private long fetchAllTeamUserCount(TeamSummaryQuery query) {
        return dsl.select(countDistinct(TB_USER_TEAM.USER_ID))
                .from(TB_TEAM)
                .join(TB_USER_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(TB_USER_TEAM.STATUS_CODE)))
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId()))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    private Condition teamScopeCondition(TbTeam team, Long departmentId, Long visibleTeamId) {
        Condition condition = teamMembershipConditionSupport.activeTeam(team.DELETED_AT);
        if (departmentId != null) {
            condition = condition.and(team.DEPARTMENT_ID.eq(departmentId));
        }
        if (visibleTeamId != null) {
            condition = condition.and(team.TEAM_ID.eq(visibleTeamId));
        }
        return condition;
    }

    private Field<Integer> allocationPriority(Field<String> allocationField) {
        return when(upper(coalesce(allocationField, inline(""))).in("PRIMARY", "MAIN", "LEAD"), inline(1))
                .otherwise(inline(0));
    }

    private Field<Integer> worklogStatusPriority(Field<String> statusCodeField) {
        return when(statusCodeField.eq(WorklogStatus.IN_PROGRESS.name()), inline(0))
                .when(statusCodeField.eq(WorklogStatus.PENDING.name()), inline(1))
                .when(statusCodeField.eq(WorklogStatus.ON_HOLD.name()), inline(2))
                .when(statusCodeField.eq(WorklogStatus.COMPLETED.name()), inline(3))
                .when(statusCodeField.eq(WorklogStatus.CANCELLED.name()), inline(4))
                .otherwise(inline(5));
    }
}
