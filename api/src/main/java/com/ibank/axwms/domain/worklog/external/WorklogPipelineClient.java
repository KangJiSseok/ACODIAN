package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.TriggerWorklogPipelineDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * AI 서버에 업무일지 본문 요약과 태그 생성 파이프라인 실행을 요청한다.
 * 결과 반영은 AI 서버가 내부 콜백 API 를 다시 호출하는 비동기 계약이다.
 */
@Component
public class WorklogPipelineClient {

    private static final String WORKLOG_PIPELINE_PATH = "/pipeline/worklog";

    private final RestClient restClient;

    public WorklogPipelineClient(AiWorklogPipelineProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** AI 서버에 업무일지 파이프라인 시작 요청을 던지고 202 응답 본문은 사용하지 않는다. */
    public void requestPipeline(TriggerWorklogPipelineDto.Request request) {
        restClient.post()
                .uri(WORKLOG_PIPELINE_PATH)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** AI 장애가 등록 후처리 경로를 오래 붙잡지 않도록 connect/read timeout 을 짧게 제한한다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiWorklogPipelineProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
