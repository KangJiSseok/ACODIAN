package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogStatusHistoryProjection;

import java.util.List;

public interface WorklogStatusHistoryJooqRepository {

    /** 주어진 worklog 의 상태 변경 이력을 시간 오름차순으로 모두 조회한다. */
    List<WorklogStatusHistoryProjection> findStatusHistories(Long worklogId);
}
