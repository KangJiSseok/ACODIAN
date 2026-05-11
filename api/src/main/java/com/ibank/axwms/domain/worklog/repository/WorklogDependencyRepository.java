package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.WorklogDependency;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogDependencyJooqRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface WorklogDependencyRepository extends JpaRepository<WorklogDependency, Long>, WorklogDependencyJooqRepository {

    /** 선행 업무 교체 시 더 이상 필요 없는 (worklogId, dependsOnWorklogId) 행을 일괄 삭제한다. */
    void deleteAllByWorklogIdAndDependsOnWorklogIdIn(Long worklogId, Collection<Long> dependsOnWorklogIds);
}
