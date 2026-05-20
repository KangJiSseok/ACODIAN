package com.ibank.axwms.domain.worklog.service;

import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogAiProcessingStatusService {

    private final WorklogRepository worklogRepository;

    /**
     * AI 재처리처럼 외부 콜백 대기를 시작해야 하는 업무를 처리중 상태로 표시한다.
     *
     * @param worklogId 상태를 변경할 업무 ID
     */
    @Transactional
    public void startAiProcessing(Long worklogId) {
        getWorklogOrThrow(worklogId).startAiProcessing();
    }

    /**
     * 통합 AI 후처리 요청이 모두 정상 dispatch 된 업무를 즉시 완료 상태로 표시한다.
     *
     * @param worklogId 상태를 변경할 업무 ID
     */
    @Transactional
    public void completeAiProcessing(Long worklogId) {
        getWorklogOrThrow(worklogId).completeAiProcessing();
    }

    /**
     * 통합 AI 후처리 요청 중 하나 이상 실패한 업무를 실패 상태로 표시한다.
     *
     * @param worklogId 상태를 변경할 업무 ID
     */
    @Transactional
    public void failAiProcessing(Long worklogId) {
        getWorklogOrThrow(worklogId).failAiProcessing();
    }

    /**
     * 후처리 이벤트가 가리키는 업무가 실제 저장된 업무인지 확인해 잘못된 상태 갱신을 차단한다.
     */
    private Worklog getWorklogOrThrow(Long worklogId) {
        return worklogRepository.findById(worklogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKLOG_NOT_FOUND));
    }
}
