package com.ibank.axwms.domain.worklog.service;

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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 업무 생성 이후 AI 파일 요약, 본문 파이프라인, Light index 요청을 하나의 결과 알림으로 묶는 후처리 서비스다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorklogAiPostProcessService {

    private static final String FILE_SUMMARY_STAGE = "FILE_SUMMARY";
    private static final String WORKLOG_PIPELINE_STAGE = "WORKLOG_PIPELINE";
    private static final String LIGHT_INDEX_STAGE = "LIGHT_INDEX";

    private final FileSummaryClient fileSummaryClient;
    private final FileService fileService;
    private final AiFileSummaryProperties fileSummaryProperties;
    private final WorklogPipelineClient worklogPipelineClient;
    private final WorklogLightIndexClient worklogLightIndexClient;
    private final AiWorklogPipelineProperties worklogPipelineProperties;
    private final AiWorklogLightIndexProperties worklogLightIndexProperties;
    private final NotificationService notificationService;

    /**
     * 커밋된 업무 snapshot 으로 세 AI 요청을 모두 시도하고, 실패 단계 집계 결과를 작성자 알림으로 남긴다.
     *
     * @param event 업무 생성 트랜잭션 안에서 확정된 AI 후처리 입력 snapshot
     */
    public void process(WorklogAiPostProcessRequestedEvent event) {
        List<String> failedStages = new ArrayList<>();

        requestFileSummaries(event, failedStages);
        requestWorklogPipeline(event, failedStages);
        requestLightIndex(event, failedStages);

        if (failedStages.isEmpty()) {
            notificationService.createWorklogAiPostProcessResultNotification(
                    event.authorId(),
                    event.departmentId(),
                    event.teamId(),
                    event.worklogId(),
                    true,
                    List.of()
            );
            return;
        }

        notificationService.createWorklogAiPostProcessResultNotification(
                event.authorId(),
                event.departmentId(),
                event.teamId(),
                event.worklogId(),
                false,
                failedStages
        );
    }

    /**
     * 첨부 파일이 없는 생성 요청은 정상 skip 으로 두고, 있는 파일은 개별 실패를 파일 ID 와 함께 집계한다.
     */
    private void requestFileSummaries(WorklogAiPostProcessRequestedEvent event, List<String> failedStages) {
        if (!fileSummaryProperties.enabled()) {
            log.debug("AI 파일 요약 통합 후처리 비활성화 worklogId={}", event.worklogId());
            return;
        }

        for (WorklogAiPostProcessRequestedEvent.FileSummaryTarget file : event.files()) {
            try {
                fileService.startWorklogFileAiSummaryProcessing(file.fileId());
                fileSummaryClient.requestSummary(new TriggerFileSummaryDto.Request(
                        file.fileId(),
                        event.worklogId(),
                        file.storageKey(),
                        file.originalName(),
                        file.fileExtension()
                ));
            } catch (RuntimeException e) {
                failedStages.add(FILE_SUMMARY_STAGE + "(" + file.fileId() + ")");
                log.warn("AI 파일 요약 통합 후처리 실패 worklogId={} fileId={} storageKey={}",
                        event.worklogId(), file.fileId(), file.storageKey(), e);
            }
        }
    }

    /**
     * 비활성화된 업무 본문 파이프라인은 운영 설정에 따른 정상 skip 으로 보고 실패 집계에서 제외한다.
     */
    private void requestWorklogPipeline(WorklogAiPostProcessRequestedEvent event, List<String> failedStages) {
        if (!worklogPipelineProperties.enabled()) {
            log.debug("AI 업무일지 파이프라인 통합 후처리 비활성화 worklogId={}", event.worklogId());
            return;
        }

        try {
            worklogPipelineClient.requestPipeline(new TriggerWorklogPipelineDto.Request(
                    event.worklogId(),
                    event.requestContent(),
                    event.workContent(),
                    event.authorId(),
                    event.teamId(),
                    event.departmentId()
            ));
        } catch (RuntimeException e) {
            failedStages.add(WORKLOG_PIPELINE_STAGE);
            log.warn("AI 업무일지 파이프라인 통합 후처리 실패 worklogId={}", event.worklogId(), e);
        }
    }

    /**
     * 비활성화된 Light index 는 운영 설정에 따른 정상 skip 으로 보고 실패 집계에서 제외한다.
     */
    private void requestLightIndex(WorklogAiPostProcessRequestedEvent event, List<String> failedStages) {
        if (!worklogLightIndexProperties.enabled()) {
            log.debug("AI Light 업무일지 index 통합 후처리 비활성화 worklogId={}", event.worklogId());
            return;
        }

        try {
            worklogLightIndexClient.requestIndex(TriggerWorklogLightIndexDto.Request.of(event.worklogId()));
        } catch (RuntimeException e) {
            failedStages.add(LIGHT_INDEX_STAGE);
            log.warn("AI Light 업무일지 index 통합 후처리 실패 worklogId={}", event.worklogId(), e);
        }
    }
}
