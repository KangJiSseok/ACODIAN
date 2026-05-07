package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.InternalSemanticWorklogSearchApiDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * API 서버의 public 검색 요청을 내부 AI 서버의 업무일지 시맨틱 검색 API로 전달한다.
 */
@Component
public class SemanticWorklogSearchClient {

    private static final String SEARCH_WORKLOGS_PATH = "/search/worklogs";

    private final RestClient restClient;

    public SemanticWorklogSearchClient(AiWorklogSearchProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** AI 서버가 반환한 ranking 결과를 그대로 받아 API 서비스 조립 단계로 넘긴다. */
    public InternalSemanticWorklogSearchApiDto.Response searchWorklogs(
            InternalSemanticWorklogSearchApiDto.Request request
    ) {
        InternalSemanticWorklogSearchApiDto.Response response = restClient.post()
                .uri(SEARCH_WORKLOGS_PATH)
                .body(request)
                .retrieve()
                .body(InternalSemanticWorklogSearchApiDto.Response.class);
        if (response == null) {
            throw new RestClientException("AI semantic search response body is empty");
        }
        return response;
    }

    /** AI 장애가 사용자 요청 전체 지연으로 번지지 않도록 연결/응답 대기 시간을 짧게 제한한다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiWorklogSearchProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
