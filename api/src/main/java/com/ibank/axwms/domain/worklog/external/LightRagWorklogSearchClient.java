package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.InternalLightRagWorklogQueryApiDto;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * API 서버의 업무일지 시맨틱 검색 요청을 AI 서버의 LightRAG v3 query API로 전달한다.
 */
@Component
public class LightRagWorklogSearchClient {

    private static final String QUERY_WORKLOGS_PATH = "/light/worklogs-v3/query";

    private final RestClient restClient;

    /** 업무일지 시맨틱 검색 설정값으로 LightRAG v3 전용 RestClient를 구성한다. */
    public LightRagWorklogSearchClient(AiWorklogSearchProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** LightRAG v3 native answer와 references를 받아 semantic 검색 응답 조립 단계로 넘긴다. */
    public InternalLightRagWorklogQueryApiDto.Response queryWorklogs(
            InternalLightRagWorklogQueryApiDto.Request request
    ) {
        try {
            InternalLightRagWorklogQueryApiDto.Response response = restClient.post()
                    .uri(QUERY_WORKLOGS_PATH)
                    .body(request)
                    .retrieve()
                    .body(InternalLightRagWorklogQueryApiDto.Response.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED, ex);
        }
    }

    /** LightRAG query가 길어질 수 있어 설정된 연결/응답 대기 시간 안에서만 기다린다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiWorklogSearchProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
