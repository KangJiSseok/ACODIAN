package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.dto.GetWorklogsApiDto;
import com.ibank.axwms.domain.worklog.policy.WorklogVisibilityScope;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogListProjection;
import org.springframework.data.domain.Page;

public interface WorklogJooqRepository {

    Page<WorklogListProjection> findWorklogPage(WorklogVisibilityScope scope, GetWorklogsApiDto.Request request);
}
