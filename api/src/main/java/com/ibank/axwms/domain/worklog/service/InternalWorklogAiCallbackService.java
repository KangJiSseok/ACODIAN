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

    /**
     * AI 요약 처리 결과를 업무일지에 반영하고 성공/실패 상태를 갱신한다.
     */
    @Transactional
    public void updateAiResult(Long worklogId, UpdateWorklogAiApiDto.Request request) {

        Worklog worklog = getWorklogOrThrow(worklogId);

        if (request.aiProcessingStatus() == AiProcessingStatus.COMPLETED) {
            worklog.changeAiSummary(request.aiSummary());
            worklog.completeAiSummaryProcessing();
        } else if (request.aiProcessingStatus() == AiProcessingStatus.FAILED) {
            worklog.failAiSummaryProcessing();
        }

    }

    /**
     * 잘못된 업무일지 ID로 AI 요약 처리 결과가 오염되지 않도록 대상 존재를 먼저 확정한다.
     */
    private Worklog getWorklogOrThrow(Long worklogId) {
        return worklogRepository.findById(worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));
    }

}
