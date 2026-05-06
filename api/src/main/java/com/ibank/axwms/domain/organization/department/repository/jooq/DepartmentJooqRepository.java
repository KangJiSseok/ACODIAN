package com.ibank.axwms.domain.organization.department.repository.jooq;

import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import java.util.List;

public interface DepartmentJooqRepository {

    /** 활성 부서 화면 상단에 필요한 부서/팀/사용자 집계를 조회한다. */
    DepartmentOverviewProjection getActiveDepartmentOverview();

    /** 활성 상태인 부서 목록과 부서장 표시 정보를 조회한다. */
    List<DepartmentListItemProjection> findActiveDepartments();

    /** 부서 삭제 차단 정책에서 사용하는 ACTIVE/non-deleted owned team 존재 여부를 조회한다. */
    boolean existsActiveOwnedTeam(Long departmentId);

    /** ACTIVE membership 기준 현재 부서에 연결된 사용자 수를 조회한다. */
    int fetchActiveUserCount(Long departmentId);
}
