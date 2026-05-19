package com.ibank.axwms.domain.worklog.external;

import com.ibank.axwms.domain.worklog.dto.InternalLightRagWorklogQueryApiDto;
import com.ibank.axwms.global.error.BusinessException;
import com.ibank.axwms.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

/**
 * API 서버의 업무일지 시맨틱 검색 요청을 AI 서버의 LightRAG v3 query API로 전달한다.
 */
@Slf4j
@Component
public class LightRagWorklogSearchClient {

    private static final String QUERY_WORKLOGS_PATH = "/light/worklogs-v3/query";

    private final RestClient restClient;
    private final String baseUrl;

    /** 업무일지 시맨틱 검색 설정값으로 LightRAG v3 전용 RestClient를 구성한다. */
    public LightRagWorklogSearchClient(AiWorklogSearchProperties properties) {
        this.baseUrl = properties.baseUrl();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** LightRAG v3 native answer와 references를 받아 semantic 검색 응답 조립 단계로 넘긴다. */
    public InternalLightRagWorklogQueryApiDto.Response queryWorklogs(
            InternalLightRagWorklogQueryApiDto.Request request
    ) {
        long startedAtNanos = System.nanoTime();
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
        } catch (RestClientResponseException ex) {
            logUpstreamHttpFailure(ex, durationMillis(startedAtNanos));
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED, ex);
        } catch (ResourceAccessException ex) {
            logUpstreamAccessFailure(ex, durationMillis(startedAtNanos));
            throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED, ex);
        } catch (RestClientException ex) {
            if (hasSocketTimeoutCause(ex)) {
                logUpstreamAccessFailure(ex, durationMillis(startedAtNanos));
                throw new BusinessException(ErrorCode.WORKLOG_SEMANTIC_SEARCH_FAILED, ex);
            }
            log.warn(
                    "LightRAG 업무일지 검색 upstream 호출 실패: path={}, baseUrl={}, durationMs={}",
                    QUERY_WORKLOGS_PATH,
                    baseUrl,
                    durationMillis(startedAtNanos),
                    ex
            );
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

    /** upstream HTTP 상태를 남겨 API 502가 AI 500/504를 가린 상황을 본문 노출 없이 구분한다. */
    private void logUpstreamHttpFailure(RestClientResponseException ex, long durationMs) {
        log.warn(
                "LightRAG 업무일지 검색 upstream HTTP 오류: path={}, baseUrl={}, status={}, durationMs={}",
                QUERY_WORKLOGS_PATH,
                baseUrl,
                ex.getStatusCode().value(),
                durationMs
        );
    }

    /** 연결 실패와 read timeout 계열 실패는 duration이 핵심 단서라 별도 로그로 분리한다. */
    private void logUpstreamAccessFailure(RestClientException ex, long durationMs) {
        log.warn(
                "LightRAG 업무일지 검색 upstream 접근 오류: path={}, baseUrl={}, durationMs={}",
                QUERY_WORKLOGS_PATH,
                baseUrl,
                durationMs,
                ex
        );
    }

    /** RestClient가 timeout을 포장하는 방식이 달라도 운영 로그에서는 read timeout 계열로 분류한다. */
    private static boolean hasSocketTimeoutCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof SocketTimeoutException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /** timeout 경계 판단에 쓰는 호출 소요 시간을 millisecond 단위로 변환한다. */
    private static long durationMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
