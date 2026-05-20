package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.InternalWorklogPolishApiDto;
import com.ibank.axwms.domain.worklog.dto.InternalWorklogTitleRecommendationApiDto;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * API 서버의 작성 보조 요청을 내부 AI 서버의 업무일지 polish/title API로 전달한다.
 */
@Component
public class WorklogPolishClient {

    private static final String WORKLOG_POLISH_PATH = "/worklogs/polish";
    private static final String WORKLOG_TITLE_RECOMMENDATIONS_PATH = "/worklogs/title-recommendations";

    private final RestClient restClient;

    public WorklogPolishClient(AiWorklogPolishProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** AI upstream 실패를 작성 화면 계약에 맞는 502 비즈니스 오류로 변환한다. */
    public InternalWorklogPolishApiDto.Response polishWorklog(
            InternalWorklogPolishApiDto.Request request
    ) {
        try {
            InternalWorklogPolishApiDto.Response response = restClient.post()
                    .uri(WORKLOG_POLISH_PATH)
                    .body(request)
                    .retrieve()
                    .body(InternalWorklogPolishApiDto.Response.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED, ex);
        }
    }

    /** AI upstream 실패를 제목 추천도 동일한 작성 보조 502 비즈니스 오류로 변환한다. */
    public InternalWorklogTitleRecommendationApiDto.Response recommendWorklogTitles(
            InternalWorklogTitleRecommendationApiDto.Request request
    ) {
        try {
            InternalWorklogTitleRecommendationApiDto.Response response = restClient.post()
                    .uri(WORKLOG_TITLE_RECOMMENDATIONS_PATH)
                    .body(request)
                    .retrieve()
                    .body(InternalWorklogTitleRecommendationApiDto.Response.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED);
            }
            return response;
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.WORKLOG_AI_POLISH_FAILED, ex);
        }
    }

    /** 사용자 대기 요청이 무기한 묶이지 않도록 작성 보조 전용 timeout 을 적용한다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiWorklogPolishProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
