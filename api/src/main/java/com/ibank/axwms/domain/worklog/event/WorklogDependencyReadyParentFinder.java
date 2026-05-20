package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyReadyParentProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorklogDependencyReadyParentFinder {

    private final WorklogDependencyRepository worklogDependencyRepository;

    /**
     * 완료 상태 커밋 이후 별도 읽기 트랜잭션에서 JOOQ가 확정된 DB 상태만 기준으로 부모 준비 여부를 판정하게 한다.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<WorklogDependencyReadyParentProjection> findReadyParents(Long completedWorklogId) {
        return worklogDependencyRepository.findReadyParentsByCompletedPredecessorId(completedWorklogId);
    }
}
