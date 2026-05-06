package com.ibank.axwms.domain.organization.department.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static org.jooq.impl.DSL.countDistinct;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailHeaderProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailTeamProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.team.UserTeamStatus;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.global.jooq.tables.TbUser;
import com.ibank.axwms.global.jooq.tables.TbUserTeam;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepartmentJooqRepositoryImpl implements DepartmentJooqRepository {

    private static final String ACTIVE_DEPARTMENT_STATUS = DepartmentStatus.ACTIVE.name();
    private static final String ACTIVE_TEAM_STATUS = TeamStatus.ACTIVE.name();
    private static final String ACTIVE_USER_TEAM_STATUS = UserTeamStatus.ACTIVE.name();
    private static final String ACTIVE_EMPLOYMENT_STATUS = EmploymentStatus.ACTIVE.name();

    private final DSLContext dsl;

    /** 활성 부서 목록 화면 상단에 필요한 부서/팀/사용자 집계를 조회한다. */
    @Override
    public DepartmentOverviewProjection getActiveDepartmentOverview() {
        return new DepartmentOverviewProjection(
                fetchActiveDepartmentCount(),
                fetchActiveTeamCount(),
                fetchActiveUserCount()
        );
    }

    /** 활성 상태의 부서 목록과 부서장 표시 정보를 조회한다. */
    @Override
    public List<DepartmentListItemProjection> findActiveDepartments() {
        TbUser headUser = TB_USER.as("head_user");

        return dsl.select(
                        TB_DEPARTMENT.DEPARTMENT_ID,
                        TB_DEPARTMENT.DEPARTMENT_NAME,
                        TB_DEPARTMENT.DESCRIPTION,
                        TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID,
                        headUser.USER_NAME,
                        TB_DEPARTMENT.CREATED_AT,
                        TB_DEPARTMENT.UPDATED_AT
                )
                .from(TB_DEPARTMENT)
                .leftJoin(headUser).on(TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID.eq(headUser.USER_ID))
                .where(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                .orderBy(TB_DEPARTMENT.DEPARTMENT_ID.asc())
                .fetch(record -> new DepartmentListItemProjection(
                        record.get(TB_DEPARTMENT.DEPARTMENT_ID),
                        record.get(TB_DEPARTMENT.DEPARTMENT_NAME),
                        record.get(TB_DEPARTMENT.DESCRIPTION),
                        record.get(TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID),
                        record.get(headUser.USER_NAME),
                        record.get(TB_DEPARTMENT.CREATED_AT),
                        record.get(TB_DEPARTMENT.UPDATED_AT)
                ));
    }

    /** ACTIVE 부서 상세 header 를 팀 row 와 분리해 팀이 없는 부서도 단건 응답을 만들 수 있게 한다. */
    @Override
    public Optional<DepartmentDetailHeaderProjection> findActiveDepartmentDetailHeader(Long departmentId) {
        TbUser headUser = TB_USER.as("head_user");

        return dsl.select(
                        TB_DEPARTMENT.DEPARTMENT_ID,
                        TB_DEPARTMENT.DEPARTMENT_NAME,
                        TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID,
                        headUser.USER_NAME
                )
                .from(TB_DEPARTMENT)
                .leftJoin(headUser).on(TB_DEPARTMENT.DEPARTMENT_HEAD_USER_ID.eq(headUser.USER_ID))
                .where(TB_DEPARTMENT.DEPARTMENT_ID.eq(departmentId))
                .and(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                .fetchOptional(record -> DepartmentDetailHeaderProjection.from(record, headUser));
    }

    /** 부서 직접 소유 팀에 단일 ACTIVE leader join 과 member count subquery 를 붙여 상세 목록으로 반환한다. */
    @Override
    public List<DepartmentDetailTeamProjection> findActiveDepartmentDetailTeams(Long departmentId) {
        TbUserTeam leaderMembership = TB_USER_TEAM.as("leader_membership");
        TbUserTeam activeMembership = TB_USER_TEAM.as("active_membership");
        TbUser leaderUser = TB_USER.as("leader_user");
        Field<Long> leaderId = leaderMembership.USER_ID.as("leader_id");
        Field<Long> memberCount = memberCountField(activeMembership);

        return dsl.select(
                        TB_TEAM.TEAM_ID,
                        TB_TEAM.TEAM_NAME,
                        leaderId,
                        leaderUser.USER_NAME,
                        TB_TEAM.START_DATE,
                        TB_TEAM.EXPECTED_END_DATE,
                        memberCount
                )
                .from(TB_TEAM)
                .leftJoin(leaderMembership).on(leaderMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .and(leaderMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                .and(leaderMembership.IS_LEADER.isTrue())
                .leftJoin(leaderUser).on(leaderUser.USER_ID.eq(leaderMembership.USER_ID))
                .where(TB_TEAM.DEPARTMENT_ID.eq(departmentId))
                .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS))
                .and(TB_TEAM.DELETED_AT.isNull())
                .orderBy(TB_TEAM.TEAM_ID.asc())
                .fetch(record -> DepartmentDetailTeamProjection.from(record, leaderId, leaderUser, memberCount));
    }

    /** 삭제 차단은 list/detail 과 같은 nullable ownership 및 ACTIVE/non-deleted 팀 기준을 재사용한다. */
    @Override
    public boolean existsActiveOwnedTeam(Long departmentId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(TB_TEAM)
                        .where(TB_TEAM.DEPARTMENT_ID.eq(departmentId))
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS))
                        .and(TB_TEAM.DELETED_AT.isNull())
        );
    }

    /** 현재 부서에 속하고 ACTIVE membership 으로 연결된 사용자 수를 조회한다. */
    @Override
    public int fetchActiveUserCount(Long departmentId) {
        return dsl.select(countDistinct(TB_USER.USER_ID))
                .from(TB_USER)
                .join(TB_USER_TEAM).on(TB_USER.USER_ID.eq(TB_USER_TEAM.USER_ID))
                .join(TB_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .where(TB_USER.DEPARTMENT_ID.eq(departmentId))
                .and(TB_USER_TEAM.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
                .and(TB_TEAM.DELETED_AT.isNull())
                .fetchSingle(0, Integer.class);
    }

    /** ACTIVE 상태의 department row 수를 계산한다. */
    private long fetchActiveDepartmentCount() {
        return dsl.selectCount()
                .from(TB_DEPARTMENT)
                .where(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** ACTIVE 부서가 직접 소유한 ACTIVE/non-deleted team 수만 계산해 detail/delete ownership 과 맞춘다. */
    private long fetchActiveTeamCount() {
        return dsl.selectCount()
                .from(TB_TEAM)
                .join(TB_DEPARTMENT).on(TB_TEAM.DEPARTMENT_ID.eq(TB_DEPARTMENT.DEPARTMENT_ID))
                .where(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS))
                .and(TB_TEAM.DELETED_AT.isNull())
                .and(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** ACTIVE team 에 최소 1개 이상 소속된 ACTIVE 사용자 수를 DISTINCT 기준으로 계산한다. */
    private long fetchActiveUserCount() {
        return dsl.select(countDistinct(TB_USER.USER_ID))
                .from(TB_USER)
                .join(TB_USER_TEAM).on(TB_USER.USER_ID.eq(TB_USER_TEAM.USER_ID))
                .join(TB_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .where(TB_USER.EMPLOYMENT_STATUS.eq(ACTIVE_EMPLOYMENT_STATUS)
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS))
                        .and(TB_USER.DEPARTMENT_ID.in(
                                dsl.select(TB_DEPARTMENT.DEPARTMENT_ID)
                                        .from(TB_DEPARTMENT)
                                        .where(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                        )))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** leader 단일 join 에 영향받지 않도록 ACTIVE membership 수는 팀별 상관 subquery 로 계산한다. */
    private Field<Long> memberCountField(TbUserTeam activeMembership) {
        return DSL.field(
                dsl.select(countDistinct(activeMembership.USER_ID).cast(Long.class))
                        .from(activeMembership)
                        .where(activeMembership.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                        .and(activeMembership.STATUS_CODE.eq(ACTIVE_USER_TEAM_STATUS))
        ).as("member_count");
    }
}
