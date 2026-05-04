package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WorklogDependencyJooqRepository {

    /** 주어진 worklog 의 직접 선행 업무 목록(depth 1)을 조회한다. */
    List<WorklogDependencyProjection> findDirectDependencies(Long worklogId);

    /**
     * 여러 worklog 의 직접 선행 업무 개수를 한 번의 쿼리로 집계해 (worklogId → count) 맵으로 반환한다.
     * 의존이 없는 worklog 는 맵에 포함되지 않으므로 호출 측에서 0 으로 보정한다.
     */
    Map<Long, Long> countByWorklogIds(Collection<Long> worklogIds);
}
