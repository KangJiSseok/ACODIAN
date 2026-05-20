package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogPipelineDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogPipelineProperties;
import com.ibank.axwms.domain.worklog.external.WorklogPipelineClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WorklogAiPipelineTriggerTest {

    @Test
    @DisplayName("파이프라인이 활성화되어 있으면 AI 서버 호출 요청을 조립한다")
    void request_pipeline_when_enabled() {
        WorklogPipelineClient client = mock(WorklogPipelineClient.class);
        WorklogAiPipelineTrigger trigger = new WorklogAiPipelineTrigger(
                client,
                new AiWorklogPipelineProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2))
        );
        WorklogAiPipelineRequestedEvent event = event();

        trigger.onCommit(event);

        ArgumentCaptor<TriggerWorklogPipelineDto.Request> captor = ArgumentCaptor.forClass(TriggerWorklogPipelineDto.Request.class);
        verify(client).requestPipeline(captor.capture());
        assertThat(captor.getValue().worklogId()).isEqualTo(event.worklogId());
        assertThat(captor.getValue().requestContent()).isEqualTo(event.requestContent());
        assertThat(captor.getValue().workContent()).isEqualTo(event.workContent());
        assertThat(captor.getValue().authorId()).isEqualTo(event.authorId());
        assertThat(captor.getValue().teamId()).isEqualTo(event.teamId());
        assertThat(captor.getValue().departmentId()).isEqualTo(event.departmentId());
    }

    @Test
    @DisplayName("파이프라인이 비활성화되어 있으면 AI 서버를 호출하지 않는다")
    void does_not_request_pipeline_when_disabled() {
        WorklogPipelineClient client = mock(WorklogPipelineClient.class);
        WorklogAiPipelineTrigger trigger = new WorklogAiPipelineTrigger(
                client,
                new AiWorklogPipelineProperties(false, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2))
        );

        trigger.onCommit(event());

        verify(client, never()).requestPipeline(org.mockito.ArgumentMatchers.any());
    }

    /**
     * listener 단위 테스트가 동일한 업무 문맥을 공유하도록 이벤트 fixture 를 고정한다.
     */
    private static WorklogAiPipelineRequestedEvent event() {
        return new WorklogAiPipelineRequestedEvent(
                501L,
                "요청 내용",
                "업무 내용",
                101L,
                21L,
                9L
        );
    }
}
