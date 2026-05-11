package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.worklog.entity.WorklogDependency;
import com.ibank.axwms.domain.worklog.repository.WorklogDependencyRepository;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogDependencyService {

    private final WorklogDependencyRepository worklogDependencyRepository;
    private final WorklogRepository worklogRepository;

    /**
     * 신규 worklog 에 선행 worklog 들을 연결한다.
     * 입력 리스트는 중복을 허용하지 않으며 검증을 통과한 unique 집합만 저장한다.
     * 검증:
     *  1) 자기참조 — predecessorIds 가 worklogId 자신을 포함하면 거부 (방어적 — create 시점엔 자기 ID 모르지만 update 재사용 대비)
     *  2) 같은 팀 — 선행 worklog 는 자식 worklog 와 동일 팀 소속이어야 한다. 존재/미삭제 검증 포함.
     *  3) 순환 의존 — 사실상 create 시점엔 새 worklog 가 누구의 선행도 아니라 사이클 발생 불가지만, update 흐름과 동일 가드를 적용
     *
     * @param worklogId 의존을 등록할 (자식) worklog ID
     * @param childTeamId 자식 worklog 의 소속 팀 ID. 선행 후보는 이 팀에 속한 worklog 여야 한다.
     * @param predecessorWorklogIds 선행으로 지정할 worklog ID 목록. null 이거나 비어 있으면 아무 작업도 하지 않는다.
     */
    @Transactional
    public void registerPredecessor(Long worklogId, Long childTeamId, List<Long> predecessorWorklogIds) {
        if (predecessorWorklogIds == null || predecessorWorklogIds.isEmpty()) {
            return;
        }

        Set<Long> uniquePredecessorIds = new LinkedHashSet<>(predecessorWorklogIds);
        validateSelfReference(worklogId, uniquePredecessorIds);
        validateSameTeam(childTeamId, uniquePredecessorIds);
        validateNoCycle(worklogId, uniquePredecessorIds);

        saveAll(worklogId, uniquePredecessorIds);
    }

    /**
     * 기존 선행 집합을 새 집합으로 교체한다 (수정 흐름 전용).
     *  - null 입력은 변경 없음 (호출 측에서 시멘틱 결정)
     *  - 빈 리스트는 모든 선행 제거를 의미
     *  - 기존과의 diff 만 처리: toAdd 는 같은 팀/자기참조/사이클 검증, toRemove 는 단순 일괄 삭제
     *
     * @param worklogId 의존 그래프의 자식 worklog ID
     * @param childTeamId 자식 worklog 의 소속 팀 ID. 새로 추가되는 선행은 이 팀 소속이어야 한다.
     * @param newPredecessorWorklogIds 새 선행 집합. null 이면 변경하지 않는다.
     */
    @Transactional
    public void replacePredecessors(Long worklogId, Long childTeamId, List<Long> newPredecessorWorklogIds) {
        if (newPredecessorWorklogIds == null) {
            return;
        }

        Set<Long> requested = new LinkedHashSet<>(newPredecessorWorklogIds);
        validateSelfReference(worklogId, requested);

        Set<Long> existing = worklogDependencyRepository.findDependsOnWorklogIdsByWorklogId(worklogId);

        Set<Long> toAdd = new LinkedHashSet<>(requested);
        toAdd.removeAll(existing);

        Set<Long> toRemove = new HashSet<>(existing);
        toRemove.removeAll(requested);

        if (!toAdd.isEmpty()) {
            validateSameTeam(childTeamId, toAdd);
            validateNoCycle(worklogId, toAdd);
            saveAll(worklogId, toAdd);
        }

        if (!toRemove.isEmpty()) {
            worklogDependencyRepository.deleteAllByWorklogIdAndDependsOnWorklogIdIn(worklogId, toRemove);
        }
    }

    /** 자기 자신을 선행으로 지정하면 거부한다. */
    private void validateSelfReference(Long worklogId, Set<Long> predecessorWorklogIds) {
        if (predecessorWorklogIds.contains(worklogId)) {
            throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_SELF_REFERENCE);
        }
    }

    /**
     * 선행 worklog 의 존재/미삭제 여부와 자식 worklog 와의 동일 팀 소속 여부를 함께 검증한다.
     * 다른 팀 worklog 를 선행으로 지정하거나, 없는/삭제된 worklog 를 지정하면 모두 동일 ErrorCode 로 거부 — 정보 누출 방지.
     */
    private void validateSameTeam(Long childTeamId, Set<Long> predecessorWorklogIds) {
        Map<Long, Long> teamIdByWorklogId = worklogRepository.findTeamIdsByWorklogIds(predecessorWorklogIds);
        if (teamIdByWorklogId.size() != predecessorWorklogIds.size()) {
            throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_NOT_ACCESSIBLE);
        }
        for (Long predTeamId : teamIdByWorklogId.values()) {
            if (!childTeamId.equals(predTeamId)) {
                throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_NOT_ACCESSIBLE);
            }
        }
    }

    /**
     * 새 선행 후보들이 의존 그래프상 candidate worklog 에 도달하는 경로를 가지면 사이클이 발생한다.
     * recursive CTE 로 한 번에 검사하고, 사이클 유발 ID 가 하나라도 있으면 거부한다.
     */
    private void validateNoCycle(Long worklogId, Set<Long> newPredecessorIds) {
        Set<Long> cycling = worklogDependencyRepository.findPredecessorsCausingCycle(worklogId, newPredecessorIds);
        if (!cycling.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_CYCLE);
        }
    }

    private void saveAll(Long worklogId, Set<Long> predecessorWorklogIds) {
        List<WorklogDependency> dependencies = predecessorWorklogIds.stream()
                .map(predecessorId -> WorklogDependency.create(worklogId, predecessorId))
                .toList();
        worklogDependencyRepository.saveAll(dependencies);
    }
}
