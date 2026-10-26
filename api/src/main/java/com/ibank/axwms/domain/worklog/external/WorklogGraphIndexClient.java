package com.ibank.axwms.domain.worklog.external;

import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 커밋된 업무일지의 GraphRAG 색인을 AI 큐에 접수한다. */
@Component
public class WorklogGraphIndexClient {
    private final RestClient restClient;

    public WorklogGraphIndexClient(AiWorklogGraphIndexProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        restClient = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    /** graph 색인과 본문 변경 시 임베딩 교체를 같은 접수 요청으로 묶는다. */
    public void requestIndex(Long worklogId, boolean refreshEmbedding) {
        restClient.post()
                .uri("/graph/worklogs-v1/index-requests")
                .body(new IndexRequest(List.of(worklogId), refreshEmbedding))
                .retrieve()
                .toBodilessEntity();
    }

    private record IndexRequest(List<Long> worklogIds, boolean refreshEmbedding) {}
}
