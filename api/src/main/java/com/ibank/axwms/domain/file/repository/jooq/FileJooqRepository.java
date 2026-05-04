package com.ibank.axwms.domain.file.repository.jooq;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogFileProjection;

import java.util.List;

public interface FileJooqRepository {

    /** 주어진 worklog 에 첨부된, 삭제되지 않은 파일 목록을 조회한다. */
    List<WorklogFileProjection> findByWorklogId(Long worklogId);
}
