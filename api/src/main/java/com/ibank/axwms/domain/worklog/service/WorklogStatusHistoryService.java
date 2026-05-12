package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.entity.WorklogStatusHistory;
import com.ibank.axwms.domain.worklog.repository.WorklogStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogStatusHistoryService {
    private final WorklogStatusHistoryRepository worklogStatusHistoryRepository;

    /**
     * 업무 등록 요청에서 확정된 최초 상태를 상태 이력의 시작점으로 기록한다.
     */
    @Transactional
    public void createStatusHistory(Long workingLogId, WorklogStatus statusCode, Long changedBy) {
        worklogStatusHistoryRepository.save(WorklogStatusHistory.create(
            workingLogId, statusCode, changedBy
        ));
    }
}
