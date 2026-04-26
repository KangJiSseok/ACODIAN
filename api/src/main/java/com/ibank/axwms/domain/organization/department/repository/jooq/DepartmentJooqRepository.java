package com.ibank.axwms.domain.organization.department.repository.jooq;

import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import java.util.List;

public interface DepartmentJooqRepository {

    /** 활성 부서 화면 상단에 필요한 부서/팀/사용자 집계를 조회한다. */
    DepartmentOverviewProjection getActiveDepartmentOverview();

    /** 활성 상태인 부서 목록과 부서장 표시 정보를 조회한다. */
    List<DepartmentListItemProjection> findActiveDepartments();
}
