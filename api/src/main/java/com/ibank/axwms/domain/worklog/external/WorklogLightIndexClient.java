package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogLightIndexDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * AI 서버의 Light v3 업무일지 index endpoint 에 색인 요청을 전달한다.
 */
@Component
public class WorklogLightIndexClient {

    private static final String WORKLOG_LIGHT_INDEX_PATH = "/light/worklogs-v3/index";

    private final RestClient restClient;

    public WorklogLightIndexClient(AiWorklogLightIndexProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** AI 서버에 업무일지 Light v3 index 반영을 요청하고 응답 본문은 사용하지 않는다. */
    public void requestIndex(TriggerWorklogLightIndexDto.Request request) {
        restClient.post()
                .uri(WORKLOG_LIGHT_INDEX_PATH)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** AI index 장애가 생성 후처리 경로를 오래 붙잡지 않도록 connect/read timeout 을 짧게 제한한다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiWorklogLightIndexProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
