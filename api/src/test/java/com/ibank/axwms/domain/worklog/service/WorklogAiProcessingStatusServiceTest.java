package com.ibank.axwms.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ibank.axwms.domain.worklog.WorklogImportance;
import com.ibank.axwms.domain.worklog.WorklogStatus;
import com.ibank.axwms.domain.worklog.entity.Worklog;
import com.ibank.axwms.domain.worklog.repository.WorklogRepository;
import com.ibank.axwms.global.enums.AiProcessingStatus;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorklogAiProcessingStatusServiceTest {

    @Mock
    private WorklogRepository worklogRepository;

    private WorklogAiProcessingStatusService service;

    @BeforeEach
    void setUp() {
        service = new WorklogAiProcessingStatusService(worklogRepository);
    }

    @Test
    @DisplayName("통합 AI 후처리 요청이 모두 dispatch 되면 업무 AI 상태를 PROCESSING으로 변경한다")
    void startAiProcessing_marks_worklog_processing() {
        Worklog worklog = worklog();
        when(worklogRepository.findById(501L)).thenReturn(Optional.of(worklog));

        service.startAiProcessing(501L);

        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.PROCESSING);
    }

    @Test
    @DisplayName("통합 AI 후처리 요청 중 하나라도 실패하면 업무 AI 상태를 FAILED로 변경한다")
    void failAiProcessing_marks_worklog_failed() {
        Worklog worklog = worklog();
        when(worklogRepository.findById(501L)).thenReturn(Optional.of(worklog));

        service.failAiProcessing(501L);

        assertThat(worklog.getAiProcessingStatus()).isEqualTo(AiProcessingStatus.FAILED);
    }

    @Test
    @DisplayName("존재하지 않는 업무의 AI 상태 갱신은 WORKLOG_NOT_FOUND로 차단한다")
    void update_status_fails_when_worklog_not_found() {
        when(worklogRepository.findById(501L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startAiProcessing(501L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKLOG_NOT_FOUND);
    }

    private static Worklog worklog() {
        return Worklog.create(
                101L,
                21L,
                "업무 제목",
                "요청 내용",
                "업무 내용",
                WorklogStatus.IN_PROGRESS,
                WorklogImportance.NORMAL,
                BigDecimal.ONE,
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );
    }
}
