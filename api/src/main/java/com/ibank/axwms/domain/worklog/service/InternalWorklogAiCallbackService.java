package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalWorklogAiCallbackService {

    private final WorklogRepository worklogRepository;

    public void updateAiResult(Long worklogId, UpdateWorklogAiApiDto.Request request) {

        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));

        if (request.aiProcessingStatus() == AiProcessingStatus.COMPLETED) {
            worklog.changeAiSummary(request.aiSummary());
            worklog.completeAiSummaryProcessing();
        } else if (request.aiProcessingStatus() == AiProcessingStatus.FAILED) {
            worklog.failAiSummaryProcessing();
        }

    }

}
