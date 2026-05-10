package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.organization.team.service.TeamService;
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
    private final TeamService teamService;

    /**
     * 신규 worklog 에 선행 worklog 들을 연결한다.
     * 입력 리스트는 중복을 허용하지 않으며 검증을 통과한 unique 집합만 저장한다.
     * 검증은 두 단계로 이루어진다.
     *  1) worklog 존재/미삭제 — worklog_id → team_id 매핑 확보
     *  2) 사용자의 각 선행 worklog 소속 팀 접근 권한 — admin grant 또는 ACTIVE membership
     * 두 단계 어느 쪽이든 실패하면 정보 누출 방지를 위해 동일한 ErrorCode 로 통합 응답한다.
     *
     * @param userId 등록 요청자의 사용자 ID
     * @param worklogId 의존을 등록할 (자식) worklog ID
     * @param predecessorWorklogIds 선행으로 지정할 worklog ID 목록. null 이거나 비어 있으면 아무 작업도 하지 않는다.
     * @throws BusinessException WORKLOG_PREDECESSOR_NOT_ACCESSIBLE 선행 worklog 가 존재하지 않거나 접근 권한이 없을 때
     */
    @Transactional
    public void registerPredecessor(Long userId, Long worklogId, List<Long> predecessorWorklogIds) {
        if (predecessorWorklogIds == null || predecessorWorklogIds.isEmpty()) {
            return;
        }

        Set<Long> uniquePredecessorIds = new LinkedHashSet<>(predecessorWorklogIds);
        validatePredecessors(userId, uniquePredecessorIds);

        List<WorklogDependency> dependencies = uniquePredecessorIds.stream()
                .map(predecessorId -> WorklogDependency.create(worklogId, predecessorId))
                .toList();
        worklogDependencyRepository.saveAll(dependencies);
    }

    /**
     * 선행 worklog 의 존재/미삭제 여부와 사용자의 팀 접근 권한을 함께 검증한다.
     * 1단계에서 매핑이 누락된 경우와 2단계에서 접근 불가 팀이 있는 경우를 동일한 ErrorCode 로 통합한다.
     */
    private void validatePredecessors(Long userId, Set<Long> predecessorWorklogIds) {
        Map<Long, Long> teamIdByWorklogId = worklogRepository.findTeamIdsByWorklogIds(predecessorWorklogIds);
        if (teamIdByWorklogId.size() != predecessorWorklogIds.size()) {
            throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_NOT_ACCESSIBLE);
        }

        Set<Long> uniqueTeamIds = new HashSet<>(teamIdByWorklogId.values());
        Set<Long> accessibleTeamIds = teamService.findAccessibleTeamIds(userId, uniqueTeamIds);
        if (!accessibleTeamIds.containsAll(uniqueTeamIds)) {
            throw new BusinessException(ErrorCode.WORKLOG_PREDECESSOR_NOT_ACCESSIBLE);
        }
    }
}
