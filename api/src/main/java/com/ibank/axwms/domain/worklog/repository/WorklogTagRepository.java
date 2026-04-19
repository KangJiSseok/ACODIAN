package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogTagJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogTagRepository extends JpaRepository<WorklogTag, Long>, WorklogTagJooqRepository {
}
