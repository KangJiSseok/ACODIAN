package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogRepository extends JpaRepository<Worklog, Long>, WorklogJooqRepository {
}
