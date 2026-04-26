package com.ibank.axwms.domain.organization.department.repository.jooq;

import static com.ibank.axwms.global.jooq.Tables.TB_DEPARTMENT;
import static com.ibank.axwms.global.jooq.Tables.TB_TEAM;
import static com.ibank.axwms.global.jooq.Tables.TB_USER;
import static com.ibank.axwms.global.jooq.Tables.TB_USER_TEAM;
import static org.jooq.impl.DSL.countDistinct;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import com.ibank.axwms.domain.organization.team.TeamStatus;
import com.ibank.axwms.domain.organization.user.EmploymentStatus;
import com.ibank.axwms.global.jooq.tables.TbUser;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepartmentJooqRepositoryImpl implements DepartmentJooqRepository {

    private static final String ACTIVE_DEPARTMENT_STATUS = DepartmentStatus.ACTIVE.name();
    private static final String ACTIVE_TEAM_STATUS = TeamStatus.ACTIVE.name();
    private static final String ACTIVE_EMPLOYMENT_STATUS = EmploymentStatus.ACTIVE.name();

    private final DSLContext dsl;

    /**
     * 활성 부서 목록 화면의 상단 집계를 조회한다.
     * 부서/팀/사용자 카운트 기준이 서로 달라 각 count 를 독립 쿼리로 계산해 의도를 명확히 유지한다.
     */
    @Override
    public DepartmentOverviewProjection getActiveDepartmentOverview() {
        return new DepartmentOverviewProjection(
                fetchActiveDepartmentCount(),
                fetchActiveTeamCount(),
                fetchActiveUserCount()
        );
    }

    /**
     * 활성 부서 목록과 부서장 표시 이름을 조회한다.
     * 부서장은 nullable 이므로 head user 는 LEFT JOIN 으로 연결하고, 화면의 안정적인 정렬을 위해 department_id 오름차순을 사용한다.
     */
    @Override
    public java.util.List<DepartmentListItemProjection> findActiveDepartments() {
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

    /** ACTIVE 상태인 department row 수를 계산한다. */
    private long fetchActiveDepartmentCount() {
        return dsl.selectCount()
                .from(TB_DEPARTMENT)
                .where(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** ACTIVE department 에 속한 ACTIVE team 수를 계산한다. */
    private long fetchActiveTeamCount() {
        return dsl.selectCount()
                .from(TB_TEAM)
                .join(TB_DEPARTMENT).on(TB_TEAM.DEPARTMENT_ID.eq(TB_DEPARTMENT.DEPARTMENT_ID))
                .where(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS)
                        .and(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }

    /** ACTIVE team 에 최소 1개 소속된 ACTIVE 사용자 수를 DISTINCT 기준으로 계산한다. */
    private long fetchActiveUserCount() {
        return dsl.select(countDistinct(TB_USER.USER_ID))
                .from(TB_USER)
                .join(TB_USER_TEAM).on(TB_USER.USER_ID.eq(TB_USER_TEAM.USER_ID))
                .join(TB_TEAM).on(TB_USER_TEAM.TEAM_ID.eq(TB_TEAM.TEAM_ID))
                .join(TB_DEPARTMENT).on(TB_TEAM.DEPARTMENT_ID.eq(TB_DEPARTMENT.DEPARTMENT_ID))
                .where(TB_USER.EMPLOYMENT_STATUS.eq(ACTIVE_EMPLOYMENT_STATUS)
                        .and(TB_TEAM.STATUS_CODE.eq(ACTIVE_TEAM_STATUS))
                        .and(TB_DEPARTMENT.STATUS_CODE.eq(ACTIVE_DEPARTMENT_STATUS)))
                .fetchSingle(0, Integer.class)
                .longValue();
    }
}
