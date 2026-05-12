package com.ibank.axwms.domain.worklog.repository.jooq;

import com.ibank.axwms.domain.worklog.repository.jooq.projection.BlockedPredecessorRowProjection;
import com.ibank.axwms.domain.worklog.repository.jooq.projection.WorklogDependencyProjection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WorklogDependencyJooqRepository {

    /** 주어진 worklog 의 직접 선행 업무 목록(depth 1)을 조회한다. */
    List<WorklogDependencyProjection> findDirectDependencies(Long worklogId);

    /**
     * 여러 worklog 의 직접 선행 업무 개수를 한 번의 쿼리로 집계해 (worklogId → count) 맵으로 반환한다.
     * 의존이 없는 worklog 는 맵에 포함되지 않으므로 호출 측에서 0 으로 보정한다.
     */
    Map<Long, Long> countByWorklogIds(Collection<Long> worklogIds);

    /**
     * 주어진 worklog id 들의 미완료 선행 worklog 쌍을 조회한다.
     * (myWorklogId, myTitle, predWorklogId, predTitle, predStatusCode) 행으로 반환되며
     * service 가 myWorklogId 로 그룹핑한다.
     */
    List<BlockedPredecessorRowProjection> findIncompletePredecessorsByWorklogIds(Collection<Long> worklogIds);

    /**
     * 새 선행 후보 ID 들에서 시작해 의존 그래프를 따라가다 candidateWorklogId 에 도달하는 경로가 존재하는지 검사한다.
     * 즉 "candidate → ... → newPredecessor" 경로가 이미 있으면 newPredecessor 를 candidate 의 선행으로 추가 시 순환이 발생한다.
     * 단일 recursive CTE 로 처리.
     *
     * @param candidateWorklogId 새 선행을 추가/대체하려는 대상 worklog
     * @param newPredecessorIds 추가될 선행 worklog ID 들 (이미 등록되어 있던 것은 제외)
     * @return 순환을 일으키는 newPredecessor ID 집합. 비어 있으면 안전.
     */
    Set<Long> findPredecessorsCausingCycle(Long candidateWorklogId, Collection<Long> newPredecessorIds);

    /**
     * 주어진 worklog 의 직접 선행 worklog ID 집합을 조회한다.
     * 선행 업무 replace 시 기존 set 과 신규 set 의 diff 계산에 사용한다.
     */
    Set<Long> findDependsOnWorklogIdsByWorklogId(Long worklogId);
}
