package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.WorklogDependency;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogDependencyJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogDependencyRepository extends JpaRepository<WorklogDependency, Long>, WorklogDependencyJooqRepository {
}
