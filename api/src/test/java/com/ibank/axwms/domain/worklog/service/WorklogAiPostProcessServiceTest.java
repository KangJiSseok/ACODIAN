package com.ibank.axwms.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.file.dto.TriggerFileSummaryDto;
import com.ibank.axwms.domain.file.external.AiFileSummaryProperties;
import com.ibank.axwms.domain.file.external.FileSummaryClient;
import com.ibank.axwms.domain.file.service.FileService;
import com.ibank.axwms.domain.notification.service.NotificationService;
import com.ibank.axwms.domain.worklog.dto.TriggerWorklogLightIndexDto;
import com.ibank.axwms.domain.worklog.dto.TriggerWorklogPipelineDto;
import com.ibank.axwms.domain.worklog.event.WorklogAiPostProcessRequestedEvent;
import com.ibank.axwms.domain.worklog.external.AiWorklogLightIndexProperties;
import com.ibank.axwms.domain.worklog.external.AiWorklogPipelineProperties;
import com.ibank.axwms.domain.worklog.external.WorklogLightIndexClient;
import com.ibank.axwms.domain.worklog.external.WorklogPipelineClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorklogAiPostProcessServiceTest {

    @Mock
    private FileSummaryClient fileSummaryClient;

    @Mock
    private FileService fileService;

    private final AiFileSummaryProperties fileSummaryProperties =
            new AiFileSummaryProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2));

    @Mock
    private WorklogPipelineClient worklogPipelineClient;

    @Mock
    private WorklogLightIndexClient worklogLightIndexClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WorklogAiProcessingStatusService worklogAiProcessingStatusService;

    private final AiWorklogPipelineProperties worklogPipelineProperties =
            new AiWorklogPipelineProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2));

    private final AiWorklogLightIndexProperties worklogLightIndexProperties =
            new AiWorklogLightIndexProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2));

    private WorklogAiPostProcessService service;

    @BeforeEach
    void setUp() {
        service = new WorklogAiPostProcessService(
                fileSummaryClient,
                fileService,
                fileSummaryProperties,
                worklogPipelineClient,
                worklogLightIndexClient,
                worklogPipelineProperties,
                worklogLightIndexProperties,
                notificationService,
                worklogAiProcessingStatusService
        );
    }

    @Test
    @DisplayName("세 AI 요청이 모두 dispatch 되면 처리중 상태와 성공 알림을 생성한다")
    void process_creates_success_notification_when_all_requests_succeed() {
        WorklogAiPostProcessRequestedEvent event = eventWithFiles();

        service.process(event);

        ArgumentCaptor<TriggerFileSummaryDto.Request> fileCaptor = ArgumentCaptor.forClass(TriggerFileSummaryDto.Request.class);
        verify(fileService).startWorklogFileAiSummaryProcessing(9001L);
        verify(fileSummaryClient).requestSummary(fileCaptor.capture());
        assertThat(fileCaptor.getValue().fileId()).isEqualTo(9001L);
        assertThat(fileCaptor.getValue().worklogId()).isEqualTo(501L);

        ArgumentCaptor<TriggerWorklogPipelineDto.Request> pipelineCaptor = ArgumentCaptor.forClass(TriggerWorklogPipelineDto.Request.class);
        verify(worklogPipelineClient).requestPipeline(pipelineCaptor.capture());
        assertThat(pipelineCaptor.getValue().worklogId()).isEqualTo(501L);
        assertThat(pipelineCaptor.getValue().departmentId()).isEqualTo(9L);

        ArgumentCaptor<TriggerWorklogLightIndexDto.Request> indexCaptor = ArgumentCaptor.forClass(TriggerWorklogLightIndexDto.Request.class);
        verify(worklogLightIndexClient).requestIndex(indexCaptor.capture());
        assertThat(indexCaptor.getValue().worklogIds()).containsExactly(501L);

        verify(worklogAiProcessingStatusService).startAiProcessing(501L);
        verify(worklogAiProcessingStatusService, never()).failAiProcessing(any());
        verify(notificationService).createWorklogAiPostProcessResultNotification(
                101L,
                9L,
                21L,
                501L,
                true,
                List.of()
        );
    }

    @Test
    @DisplayName("하나라도 실패하면 실패 단계가 포함된 실패 알림을 생성한다")
    void process_creates_failure_notification_when_any_request_fails() {
        WorklogAiPostProcessRequestedEvent event = eventWithFiles();
        doThrow(new IllegalStateException("AI down")).when(worklogPipelineClient).requestPipeline(any());

        service.process(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> failedStagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(worklogAiProcessingStatusService).failAiProcessing(501L);
        verify(worklogAiProcessingStatusService, never()).startAiProcessing(any());
        verify(notificationService).createWorklogAiPostProcessResultNotification(
                eq(101L),
                eq(9L),
                eq(21L),
                eq(501L),
                eq(false),
                failedStagesCaptor.capture()
        );
        assertThat(failedStagesCaptor.getValue()).containsExactly("WORKLOG_PIPELINE");
        verify(worklogLightIndexClient).requestIndex(any());
    }

    @Test
    @DisplayName("파일이 없으면 파일 요약 호출 없이 처리중 상태와 성공 알림을 생성한다")
    void process_skips_file_summary_when_no_files() {
        WorklogAiPostProcessRequestedEvent event = eventWithoutFiles();

        service.process(event);

        verify(fileSummaryClient, never()).requestSummary(any());
        verify(worklogPipelineClient).requestPipeline(any());
        verify(worklogLightIndexClient).requestIndex(any());
        verify(worklogAiProcessingStatusService).startAiProcessing(501L);
        verify(worklogAiProcessingStatusService, never()).failAiProcessing(any());
        verify(notificationService).createWorklogAiPostProcessResultNotification(
                101L,
                9L,
                21L,
                501L,
                true,
                List.of()
        );
    }


    @Test
    @DisplayName("파일 요약이 비활성화되어 있으면 파일 요약 호출만 건너뛰고 처리중 상태로 처리한다")
    void process_skips_file_summary_when_file_summary_disabled() {
        WorklogAiPostProcessService disabledService = new WorklogAiPostProcessService(
                fileSummaryClient,
                fileService,
                new AiFileSummaryProperties(false, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2)),
                worklogPipelineClient,
                worklogLightIndexClient,
                worklogPipelineProperties,
                worklogLightIndexProperties,
                notificationService,
                worklogAiProcessingStatusService
        );

        disabledService.process(eventWithFiles());

        verify(fileService, never()).startWorklogFileAiSummaryProcessing(any());
        verify(fileSummaryClient, never()).requestSummary(any());
        verify(worklogAiProcessingStatusService).startAiProcessing(501L);
        verify(worklogAiProcessingStatusService, never()).failAiProcessing(any());
        verify(notificationService).createWorklogAiPostProcessResultNotification(
                101L,
                9L,
                21L,
                501L,
                true,
                List.of()
        );
    }

    private static WorklogAiPostProcessRequestedEvent eventWithFiles() {
        return new WorklogAiPostProcessRequestedEvent(
                501L,
                "요청 내용",
                "업무 내용",
                101L,
                21L,
                9L,
                List.of(new WorklogAiPostProcessRequestedEvent.FileSummaryTarget(
                        9001L,
                        "worklog/501/report.txt",
                        "report.txt",
                        "txt"
                ))
        );
    }

    private static WorklogAiPostProcessRequestedEvent eventWithoutFiles() {
        return new WorklogAiPostProcessRequestedEvent(
                501L,
                "요청 내용",
                "업무 내용",
                101L,
                21L,
                9L,
                List.of()
        );
    }
}
