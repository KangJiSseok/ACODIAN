package com.ibank.axwms.domain.organization.department.repository;

import com.ibank.axwms.domain.organization.department.DepartmentStatus;
import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.jooq.DepartmentJooqRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long>, DepartmentJooqRepository {

    /** 부서명 단건 탐색. department_name 은 tb_department 의 UNIQUE key 이므로 최대 1건이 반환된다. */
    Optional<Department> findByDepartmentName(String departmentName);

    /** 신규/수정 유스케이스에서 부서명 UNIQUE 충돌을 사전에 감지한다. */
    boolean existsByDepartmentName(String departmentName);

    /** 이미 다른 부서에 head 로 지정된 사용자인지 확인한다. */
    boolean existsByDepartmentHeadUserId(Long departmentHeadUserId);

    /** 활성 부서 수정 유스케이스에서 대상 부서를 찾는다. */
    Optional<Department> findByIdAndStatusCode(Long id, DepartmentStatus statusCode);

    /** 부서 선택 후보처럼 단순 엔티티 조회로 충분한 화면에서 ACTIVE 부서를 ID 오름차순으로 찾는다. */
    List<Department> findAllByStatusCodeOrderByIdAsc(DepartmentStatus statusCode);

    /** 부서 선택 후보처럼 단순 엔티티 조회로 충분한 화면에서 지정 ACTIVE 부서만 ID 오름차순으로 찾는다. */
    List<Department> findAllByIdAndStatusCodeOrderByIdAsc(Long id, DepartmentStatus statusCode);

    /** 자기 자신을 제외한 동일 부서명 존재 여부를 확인한다. */
    boolean existsByDepartmentNameAndIdNot(String departmentName, Long id);

    /** 자기 자신을 제외한 다른 부서의 head 충돌 여부를 확인한다. */
    boolean existsByDepartmentHeadUserIdAndIdNot(Long departmentHeadUserId, Long id);
}
