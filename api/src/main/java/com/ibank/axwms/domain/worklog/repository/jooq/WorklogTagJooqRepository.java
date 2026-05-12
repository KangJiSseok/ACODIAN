package com.ibank.axwms.domain.worklog.repository.jooq;

import java.util.List;

public interface WorklogTagJooqRepository {

    /** 주어진 worklog 에 등록된 태그 이름 목록을 조회한다. */
    List<String> findTagNames(Long worklogId);
}
