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

    @Transactional
    public void createStatusHistory(Long workingLogId, Long changedBy) {
        worklogStatusHistoryRepository.save(WorklogStatusHistory.create(
            workingLogId, WorklogStatus.PENDING, changedBy
        ));
    }
}
