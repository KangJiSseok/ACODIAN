package com.ibank.axwms.domain.organization.department.repository;

import com.ibank.axwms.domain.organization.department.entity.Department;
import com.ibank.axwms.domain.organization.department.repository.jooq.DepartmentJooqRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long>, DepartmentJooqRepository {

    /** 부서명 단건 탐색. department_name 은 tb_department 의 UNIQUE key 이므로 최대 1건이 반환된다. */
    Optional<Department> findByDepartmentName(String departmentName);
}
