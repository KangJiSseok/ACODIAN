package com.ibank.axwms.domain.worklog.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibank.axwms.domain.worklog.external.AiWorklogGraphIndexProperties;
import com.ibank.axwms.domain.worklog.external.WorklogGraphIndexClient;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorklogGraphIndexTriggerTest {
    @Mock
    private WorklogGraphIndexClient client;

    @Test
    @DisplayName("GraphRAG 색인이 활성화되면 커밋된 업무 ID와 임베딩 갱신 여부를 전송한다")
    void enabledDispatchesIndexRequest() {
        var properties = new AiWorklogGraphIndexProperties(true, "http://localhost:8000/ai", Duration.ofSeconds(2), Duration.ofSeconds(3));
        new WorklogGraphIndexTrigger(client, properties)
                .onCommit(new WorklogGraphIndexRequestedEvent(101L, true));
        verify(client).requestIndex(101L, true);
    }

    @Test
    @DisplayName("GraphRAG 색인이 비활성화되면 요청을 보내지 않는다")
    void disabledSkipsIndexRequest() {
        var properties = new AiWorklogGraphIndexProperties(false, "http://localhost:8000/ai", Duration.ofSeconds(2), Duration.ofSeconds(3));
        new WorklogGraphIndexTrigger(client, properties)
                .onCommit(new WorklogGraphIndexRequestedEvent(101L, false));
        verify(client, never()).requestIndex(101L, false);
    }
}
