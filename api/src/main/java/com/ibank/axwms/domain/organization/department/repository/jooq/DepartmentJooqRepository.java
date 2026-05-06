package com.ibank.axwms.domain.organization.department.repository.jooq;

import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailHeaderProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentDetailTeamProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentListItemProjection;
import com.ibank.axwms.domain.organization.department.repository.jooq.projection.DepartmentOverviewProjection;
import java.util.List;
import java.util.Optional;

public interface DepartmentJooqRepository {

    /** 활성 부서 화면 상단에 필요한 부서/팀/사용자 집계를 조회한다. */
    DepartmentOverviewProjection getActiveDepartmentOverview();

    /** 활성 상태인 부서 목록과 부서장 표시 정보를 조회한다. */
    List<DepartmentListItemProjection> findActiveDepartments();

    /** ACTIVE 부서 상세 header 를 팀 존재 여부와 무관하게 단건 조회한다. */
    Optional<DepartmentDetailHeaderProjection> findActiveDepartmentDetailHeader(Long departmentId);

    /** nullable department ownership 로 연결된 ACTIVE/non-deleted 팀 상세 행을 teamId 오름차순으로 조회한다. */
    List<DepartmentDetailTeamProjection> findActiveDepartmentDetailTeams(Long departmentId);

    /** 부서 삭제 차단 정책에서 사용하는 ACTIVE/non-deleted owned team 존재 여부를 조회한다. */
    boolean existsActiveOwnedTeam(Long departmentId);

    /** ACTIVE membership 기준 현재 부서에 연결된 사용자 수를 조회한다. */
    int fetchActiveUserCount(Long departmentId);
}
