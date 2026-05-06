package com.ibank.axwms.domain.worklog.repository;

import com.ibank.axwms.domain.worklog.entity.WorklogTag;
import com.ibank.axwms.domain.worklog.repository.jooq.WorklogTagJooqRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklogTagRepository extends JpaRepository<WorklogTag, Long>, WorklogTagJooqRepository {

    /**
     * AI 콜백 재시도 시 이미 연결된 태그를 제외하기 위해 업무일지-태그 관계를 조회한다.
     */
    List<WorklogTag> findByWorklogIdAndTagIdIn(Long worklogId, Collection<Long> tagIds);
}
