package com.ibank.axwms.domain.organization.team.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_WORKLOG;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.when;

import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamAuthority;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamDetailProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamListProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamSummaryProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.projection.TeamWorklogProjection;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamPageQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamSummaryQuery;
import com.ibank.axwms.domain.organization.team.repository.jooq.query.TeamWorklogsQuery;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.global.jooq.tables.TbTeam;
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

    /**
     * 팀 목록 페이지를 최신 spec 필드 구조로 조립한다.
     * 호출자 membership 정보를 함께 LEFT JOIN 해 고정 정렬 우선순위와 my-* 필드를 동시에 계산한다.
     */
    @Override
    public Page<TeamListProjection> findTeamPage(TeamPageQuery query) {
        var department = TB_DEPARTMENT.as("department");
        var departmentHead = TB_USER.as("department_head");
        var leaderUser = TB_USER.as("leader_user");
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        TbUserTeam activeMembership = TB_USER_TEAM.as("active_membership");
        TbUserTeam selfMembership = TB_USER_TEAM.as("self_membership");

        Field<Integer> memberCount = countDistinct(activeMembership.USER_ID).cast(Integer.class).as("member_count");
        Field<String> myTeamAuthority = selfMembership.TEAM_AUTHORITY.as("my_team_authority");
        Field<String> myAllocation = selfMembership.ALLOCATION.as("my_allocation");
        Field<Boolean> myPrimary = coalesce(selfMembership.IS_PRIMARY, inline(false)).as("my_primary");

        Condition scopeCondition = teamScopeCondition(TB_TEAM, query);

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
                        myTeamAuthority,
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
                        .and(leaderMembership.STATUS_CODE.eq(UserTeamStatus.ACTIVE.name()))
                        .and(leaderMembership.TEAM_AUTHORITY.eq(UserTeamAuthority.LEADER.name())))
                .leftJoin(leaderUser).on(leaderMembership.USER_ID.eq(leaderUser.USER_ID))
                .leftJoin(activeMembership).on(activeMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(activeMembership.STATUS_CODE.eq(UserTeamStatus.ACTIVE.name())))
                .leftJoin(selfMembership).on(selfMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(selfMembership.STATUS_CODE.eq(UserTeamStatus.ACTIVE.name()))
                        .and(selfMembership.USER_ID.eq(query.principalUserId())))
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
                        selfMembership.TEAM_AUTHORITY,
                        selfMembership.TEAM_ROLE,
                        selfMembership.ALLOCATION,
                        selfMembership.IS_PRIMARY,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE
                )
                .orderBy(
                        TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS).desc(),
                        membershipAuthorityPriority(selfMembership.TEAM_AUTHORITY).asc(),
                        coalesce(selfMembership.IS_PRIMARY, inline(false)).desc(),
                        TB_TEAM.TEAM_ID.asc()
                )
                .limit(query.pageSize())
                .offset((query.page() - 1) * query.pageSize())
                .fetch(this::toTeamListProjection);

        return new PageImpl<>(items, PageRequest.of(query.page() - 1, query.pageSize()), totalCount);
    }

    /** visible scope 기준으로 팀 목록 상단 KPI 집계를 각각 계산해 반환한다. */
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

    /** 단일 팀 상세에 필요한 기본 정보와 soft-delete 제외 업무일지 집계를 함께 조회한다. */
    @Override
    public Optional<TeamDetailProjection> findTeamDetail(Long teamId) {
        var department = TB_DEPARTMENT.as("department");
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        var leaderUser = TB_USER.as("leader_user");

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
                        .and(leaderMembership.TEAM_AUTHORITY.eq(UserTeamAuthority.LEADER.name())))
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

    /** 단일 팀의 업무일지를 spec 우선순위 정렬로 조회한다. */
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

    /** jOOQ tuple 결과를 team 목록 projection 으로 명시적으로 매핑한다. */
    private TeamListProjection toTeamListProjection(Record17<Long, String, String, Long, String, String, Long, String, Long, String, Integer, String, String, String, Boolean, java.time.LocalDate, java.time.LocalDate> record) {
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
                record.value12() == null ? null : UserTeamAuthority.valueOf(record.value12()),
                record.value13(),
                record.value14(),
                record.value15(),
                record.value16(),
                record.value17()
        );
    }

    /** 목록 정렬에서 사용할 membership 권한 우선순위를 계산한다. */
    private Field<Integer> membershipAuthorityPriority(Field<String> authorityField) {
        return when(authorityField.eq(UserTeamAuthority.LEADER.name()), inline(0))
                .when(authorityField.eq(UserTeamAuthority.MEMBER.name()), inline(1))
                .when(authorityField.eq(UserTeamAuthority.ADMIN.name()), inline(2))
                .otherwise(inline(3));
    }

    /** visible scope 안에서 ACTIVE 상태인 팀 수를 계산한다. */
    private long fetchActiveTeamCount(TeamSummaryQuery query) {
        return dsl.selectCount()
                .from(TB_TEAM)
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId())
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** visible scope 안에서 soft-delete 되지 않은 전체 팀 수를 계산한다. */
    private long fetchTotalTeamCount(TeamSummaryQuery query) {
        return dsl.selectCount()
                .from(TB_TEAM)
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId()))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** visible scope 안에서 재직 상태가 ACTIVE 인 DISTINCT 사용자 수를 계산한다. */
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

    /** visible scope 안에서 ACTIVE 팀에 속한 DISTINCT 사용자 수를 계산한다. */
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

    /** visible scope 안에서 ACTIVE/INACTIVE 팀 전체에 속한 DISTINCT 사용자 수를 계산한다. */
    private long fetchAllTeamUserCount(TeamSummaryQuery query) {
        return dsl.select(countDistinct(TB_USER_TEAM.USER_ID))
                .from(TB_TEAM)
                .join(TB_USER_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID)
                        .and(teamMembershipConditionSupport.activeMembership(TB_USER_TEAM.STATUS_CODE)))
                .where(teamScopeCondition(TB_TEAM, query.departmentId(), query.visibleTeamId()))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** soft-delete 와 visibility 규칙을 함께 반영한 공통 팀 범위 조건을 만든다. */
    private Condition teamScopeCondition(TbTeam team, Long departmentId, Long visibleTeamId) {
        Condition condition = team.DELETED_AT.isNull();
        if (departmentId != null) {
            condition = condition.and(team.DEPARTMENT_ID.eq(departmentId));
        }
        if (visibleTeamId != null) {
            condition = condition.and(team.TEAM_ID.eq(visibleTeamId));
        }
        return condition;
    }

    /** GET /api/teams visible scope 는 역할별 기본 범위 + optional department 필터의 교집합으로 계산한다. */
    private Condition teamScopeCondition(TbTeam team, TeamPageQuery query) {
        return teamScopeCondition(team, query.departmentId(), null)
                .and(visibleScopeCondition(team, query));
    }

    /** 역할별 목록 조회 scope 를 DISTINCT team 기준 EXISTS 조건으로 정규화한다. */
    private Condition visibleScopeCondition(TbTeam team, TeamPageQuery query) {
        return switch (query.principalRole()) {
            case DIRECTOR -> trueCondition();
            case DEPT_HEAD -> team.DEPARTMENT_ID.eq(query.principalDepartmentId())
                    .or(activeMembershipExists(team, query.principalUserId()));
            case TEAM_LEAD, MEMBER -> activeMembershipExists(team, query.principalUserId());
            default -> throw new IllegalArgumentException("지원하지 않는 팀 목록 조회 역할입니다: " + query.principalRole());
        };
    }

    /** 현재 사용자의 ACTIVE membership 존재 여부를 team 단위 scope 판정에 재사용한다. */
    private Condition activeMembershipExists(TbTeam team, Long principalUserId) {
        TbUserTeam visibleMembership = TB_USER_TEAM.as("visible_membership");
        return exists(
                dsl.selectOne()
                        .from(visibleMembership)
                        .where(visibleMembership.TEAM_ID.eq(team.TEAM_ID))
                        .and(visibleMembership.USER_ID.eq(principalUserId))
                        .and(teamMembershipConditionSupport.activeMembership(visibleMembership.STATUS_CODE))
        );
    }

    /** 업무일지 목록의 spec 고정 상태 우선순위를 정수 값으로 변환한다. */
    private Field<Integer> worklogStatusPriority(Field<String> statusCodeField) {
        return when(statusCodeField.eq(WorklogStatus.IN_PROGRESS.name()), inline(0))
                .when(statusCodeField.eq(WorklogStatus.PENDING.name()), inline(1))
                .when(statusCodeField.eq(WorklogStatus.ON_HOLD.name()), inline(2))
                .when(statusCodeField.eq(WorklogStatus.COMPLETED.name()), inline(3))
                .when(statusCodeField.eq(WorklogStatus.CANCELLED.name()), inline(4))
                .otherwise(inline(5));
    }
}
