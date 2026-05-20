package com.ibank.axwms.domain.worklog.repository.jooq;

import java.util.List;

public interface WorklogTagJooqRepository {

    /** 주어진 worklog 에 등록된 태그 이름 목록을 조회한다. */
    List<String> findTagNames(Long worklogId);

    /**
     * source 태그 연결을 target 태그 연결로 합치되, 이미 target 이 있는 업무일지는 source 연결만 제거한다.
     */
    void replaceSourceTagsWithTarget(Long targetTagId, List<Long> sourceTagIds);

    /**
     * 태그 사용 횟수 캐시를 업무일지 연결의 distinct worklog 수 기준으로 재계산한다.
     */
    int countDistinctWorklogsByTagId(Long tagId);
}
