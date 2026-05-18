package com.ibank.axwms.domain.worklog.event;

import java.util.List;

/**
 * 업무 생성 커밋 뒤 통합 AI 후처리가 필요한 snapshot 을 담는다.
 * 파일 요약, 본문 파이프라인, Light index 요청을 한 listener 에서 묶어 업무당 알림을 1개로 제한한다.
 */
public record WorklogAiPostProcessRequestedEvent(
        Long worklogId,
        String requestContent,
        String workContent,
        Long authorId,
        Long teamId,
        Long departmentId,
        List<FileSummaryTarget> files
) {

    public WorklogAiPostProcessRequestedEvent {
        files = files == null ? List.of() : List.copyOf(files);
    }

    /** AI 파일 요약 요청에 필요한 파일별 snapshot 이다. */
    public record FileSummaryTarget(
            Long fileId,
            String storageKey,
            String originalName,
            String fileExtension
    ) {
    }
}
