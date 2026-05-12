package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.WorklogStatusHistory;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogStatusHistoryJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogStatusHistoryRepository extends JpaRepository<WorklogStatusHistory, Long>, WorklogStatusHistoryJooqRepository {
}
