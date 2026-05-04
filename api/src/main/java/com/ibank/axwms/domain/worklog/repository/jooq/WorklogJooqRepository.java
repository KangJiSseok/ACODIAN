package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogPageQuery;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogSearchProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.query.WorklogSearchQuery;
import org.springframework.data.domain.Page;

public interface WorklogJooqRepository {

    Page<WorklogListProjection> findWorklogPage(Long userId, WorklogPageQuery worklogPageQuery);

    /** 가시 범위와 정규화된 검색 query 로 업무일지 페이지를 조회한다. */
    Page<WorklogSearchProjection> searchWorklogPage(WorklogVisibilityScope scope, WorklogSearchQuery query);
}
