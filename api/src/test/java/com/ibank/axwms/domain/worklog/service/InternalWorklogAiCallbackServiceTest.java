package com.ibank.axwms.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.dto.UpdateWorklogAiApiDto;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalWorklogAiCallbackServiceTest {

    private static final Long WORKLOG_ID = 501L;
    private static final String AI_SUMMARY = "AI가 업무 내용을 요약한 결과";

    @Mock
    private WorklogRepository worklogRepository;

    @InjectMocks
    private InternalWorklogAiCallbackService internalWorklogAiCallbackService;

    @Test
    @DisplayName("AI 상태가 COMPLETED 이면 업무일지에 요약문과 완료 상태를 반영한다")
    void AI_상태가_completed_이면_업무일지에_요약문과_완료_상태를_반영한다() {
        Worklog worklog = createWorklog();
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request);

        assertThat(worklog.getAiSummary()).isEqualTo(AI_SUMMARY);
        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.COMPLETED);
        assertThat(worklog.getAiSummaryEdited()).isFalse();
    }

    @Test
    @DisplayName("AI 상태가 FAILED 이면 업무일지에 실패 상태만 반영한다")
    void AI_상태가_failed_이면_업무일지에_실패_상태만_반영한다() {
        Worklog worklog = createWorklog();
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.FAILED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.of(worklog));

        internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request);

        assertThat(worklog.getAiSummary()).isNull();
        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.FAILED);
        assertThat(worklog.getAiSummaryEdited()).isFalse();
    }

    @Test
    @DisplayName("업무일지가 없으면 WORKLOG_NOT_FOUND 를 던진다")
    void 업무일지가_없으면_worklog_not_found_를_던진다() {
        UpdateWorklogAiApiDto.Request request = new UpdateWorklogAiApiDto.Request(
                AI_SUMMARY,
                AiProcessingStatus.COMPLETED
        );
        given(worklogRepository.findById(WORKLOG_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> internalWorklogAiCallbackService.updateAiResult(WORKLOG_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.WORKLOG_NOT_FOUND);

        verify(worklogRepository).findById(WORKLOG_ID);
        verifyNoMoreInteractions(worklogRepository);
    }

    private Worklog createWorklog() {
        return Worklog.create(
                101L,
                21L,
                "결산 보고서 작성",
                "재무팀 요청사항 반영",
                "데이터 집계와 초안 작성",
                WorklogImportance.HIGH,
                LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 25)
        );
    }
}
