package com.ibank.axwms.domain.organization.user.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;

import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.domain.organization.user.UserRole;
import com.ibank.axwms.domain.organization.user.repository.jooq.projection.UserSummaryProjection;
import com.ibank.axwms.domain.organization.user.repository.jooq.query.UserListQuery;
import com.ibank.axwms.global.jooq.tables.TbTeam;
import com.ibank.axwms.global.jooq.tables.TbUserTeam;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserJooqRepositoryImpl implements UserJooqRepository {

    private static final String ACTIVE_EMPLOYMENT_STATUS = EmploymentStatus.ACTIVE.name();
    private static final String LEAVE_EMPLOYMENT_STATUS = EmploymentStatus.LEAVE.name();
    private static final String RETIRED_EMPLOYMENT_STATUS = EmploymentStatus.RETIRED.name();
    private static final String ACTIVE_USER_TEAM_STATUS = UserTeamStatus.ACTIVE.name();

    private final DSLContext dsl;

    /** 사용자 목록을 부서/직급/재직 상태 optional filter 와 role 우선순위 기준으로 조회한다. */
    @Override
    public List<UserSummaryProjection> findUsers(UserListQuery query) {
        TbUserTeam primaryMembership = TB_USER_TEAM.as("primary_membership");
        TbTeam primaryTeam = TB_TEAM.as("primary_team");
        Field<Long> teamId = primaryTeam.TEAM_ID.as("team_id");
        Field<String> teamName = primaryTeam.TEAM_NAME.as("team_name");
        Field<Integer> rolePriority = rolePriority();

        return dsl.select(
                        TB_USER.USER_ID,
                        TB_USER.USER_NAME,
                        TB_USER.EMAIL,
                        TB_USER.PHONE,
                        TB_USER.DEPARTMENT_ID,
                        TB_DEPARTMENT.DEPARTMENT_NAME,
                        TB_USER.PROFILE_IMAGE_URL,
                        teamId,
                        teamName,
                        TB_USER.POSITION_NAME,
                        TB_USER.TITLE_NAME,
                        TB_USER.EMPLOYMENT_STATUS,
                        rolePriority
                )
                .from(TB_USER)
                .join(TB_DEPARTMENT).on(TB_DEPARTMENT.DEPARTMENT_ID.eq(TB_USER.DEPARTMENT_ID))
                .leftJoin(primaryMembership).on(primaryMembership.USER_ID.eq(TB_USER.USER_ID)
                        .and(primaryMembership.IS_PRIMARY.isTrue())
                        .and(primaryMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS)))
                .leftJoin(primaryTeam).on(primaryTeam.TEAM_ID.eq(primaryMembership.TEAM_ID)
                        .and(primaryTeam.DELETED_AT.isNull()))
                .where(findUsersFilters(query))
                .orderBy(rolePriority.asc(), TB_USER.USER_ID.asc())
                .fetch(UserSummaryProjection::from);
    }

    /** role_code 고정 정렬 우선순위를 CASE expression 으로 만든다. */
    private Field<Integer> rolePriority() {
        return DSL.when(TB_USER.ROLE_CODE.eq(UserRole.DIRECTOR.name()), 0)
                .when(TB_USER.ROLE_CODE.eq(UserRole.DEPT_HEAD.name()), 1)
                .when(TB_USER.ROLE_CODE.eq(UserRole.TEAM_LEAD.name()), 2)
                .when(TB_USER.ROLE_CODE.eq(UserRole.MEMBER.name()), 3)
                .otherwise(4)
                .as("role_priority");
    }

    /** findUsers filter 와 RETIRED 상시 제외 규칙을 조합한다. */
    private List<Condition> findUsersFilters(UserListQuery query) {
        List<Condition> conditions = new ArrayList<>();
        addRetiredExclusion(conditions);
        addUserNameFilter(conditions, query);
        addDepartmentFilter(conditions, query);
        addPositionFilter(conditions, query);
        addEmploymentStatusFilter(conditions, query);
        return conditions;
    }

    /** 퇴직자는 명시 필터와 무관하게 사용자 목록에서 항상 제외한다. */
    private void addRetiredExclusion(List<Condition> conditions) {
        conditions.add(TB_USER.EMPLOYMENT_STATUS.ne(RETIRED_EMPLOYMENT_STATUS));
    }

    /** 사용자명이 전달된 경우에만 정확히 일치하는 사용자로 제한한다. */
    private void addUserNameFilter(List<Condition> conditions, UserListQuery query) {
        if (query.userName() != null) {
            conditions.add(TB_USER.USER_NAME.eq(query.userName()));
        }
    }

    /** 부서 id 가 전달된 경우에만 해당 부서 소속 사용자로 제한한다. */
    private void addDepartmentFilter(List<Condition> conditions, UserListQuery query) {
        if (query.departmentId() != null) {
            conditions.add(TB_USER.DEPARTMENT_ID.eq(query.departmentId()));
        }
    }

    /** 직급명이 전달된 경우에만 정확히 일치하는 사용자로 제한한다. */
    private void addPositionFilter(List<Condition> conditions, UserListQuery query) {
        if (query.positionName() != null) {
            conditions.add(TB_USER.POSITION_NAME.eq(query.positionName()));
        }
    }

    /** 재직 상태 필터가 없으면 ACTIVE/LEAVE 기본 조회 조건을 적용한다. */
    private void addEmploymentStatusFilter(List<Condition> conditions, UserListQuery query) {
        if (query.employmentStatus() == null) {
            conditions.add(TB_USER.EMPLOYMENT_STATUS.in(ACTIVE_EMPLOYMENT_STATUS, LEAVE_EMPLOYMENT_STATUS));
            return;
        }
        conditions.add(TB_USER.EMPLOYMENT_STATUS.eq(query.employmentStatus().name()));
    }
}
