package com.ibank.axwms.domain.worklog.event;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogLightIndexDto;
import com.ibank.axwms.domain.worklog.external.AiWorklogLightIndexProperties;
import com.ibank.axwms.domain.worklog.external.WorklogLightIndexClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WorklogLightIndexTriggerTest {

    @Test
    @DisplayName("Light index가 활성화되어 있으면 업무 ID를 worklogIds 배열로 감싼다")
    void request_light_index_when_enabled() {
        WorklogLightIndexClient client = mock(WorklogLightIndexClient.class);
        WorklogLightIndexTrigger trigger = new WorklogLightIndexTrigger(
                client,
                new AiWorklogLightIndexProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2))
        );

        trigger.onCommit(new WorklogLightIndexRequestedEvent(501L));

        ArgumentCaptor<TriggerWorklogLightIndexDto.Request> captor =
                ArgumentCaptor.forClass(TriggerWorklogLightIndexDto.Request.class);
        verify(client).requestIndex(captor.capture());
        assertThat(captor.getValue().worklogIds()).containsExactly(501L);
    }

    @Test
    @DisplayName("Light index가 비활성화되어 있으면 AI 서버를 호출하지 않는다")
    void does_not_request_light_index_when_disabled() {
        WorklogLightIndexClient client = mock(WorklogLightIndexClient.class);
        WorklogLightIndexTrigger trigger = new WorklogLightIndexTrigger(
                client,
                new AiWorklogLightIndexProperties(false, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2))
        );

        trigger.onCommit(new WorklogLightIndexRequestedEvent(501L));

        verify(client, never()).requestIndex(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Light index 요청 실패는 listener 밖으로 전파하지 않는다")
    void does_not_propagate_light_index_failure() {
        WorklogLightIndexClient client = mock(WorklogLightIndexClient.class);
        doThrow(new IllegalStateException("AI down")).when(client)
                .requestIndex(org.mockito.ArgumentMatchers.any());
        WorklogLightIndexTrigger trigger = new WorklogLightIndexTrigger(
                client,
                new AiWorklogLightIndexProperties(true, "http://ai.test/ai", Duration.ofSeconds(1), Duration.ofSeconds(2))
        );

        trigger.onCommit(new WorklogLightIndexRequestedEvent(501L));

        verify(client).requestIndex(org.mockito.ArgumentMatchers.any());
    }
}
