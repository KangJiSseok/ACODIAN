package com.ibank.axwms.domain.tag.external;

import com.ibank.axwms.domain.tag.dto.TagMergeCandidateApiDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * AI 서버에서 현재 태그 풀 기반 병합 후보를 생성해 Spring 운영 검토 API 로 가져온다.
 */
@Component
public class TagMergeAiClient {

    private static final String TAG_MERGE_CANDIDATES_PATH = "/tags/merge-candidates";

    private final RestClient restClient;

    public TagMergeAiClient(AiTagMergeProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /** AI 응답이 비어 있으면 저장할 후보가 없는 정상 응답으로 취급한다. */
    public TagMergeCandidateApiDto.Request generateCandidates(TagMergeCandidateApiDto.GenerateRequest request) {
        AiGenerateResponse response = restClient.post()
                .uri(TAG_MERGE_CANDIDATES_PATH)
                .body(AiGenerateRequest.from(request))
                .retrieve()
                .body(AiGenerateResponse.class);
        if (response == null || response.items() == null) {
            return new TagMergeCandidateApiDto.Request(List.of());
        }
        return new TagMergeCandidateApiDto.Request(response.items());
    }

    /** AI 후보 생성은 LLM 호출을 포함하므로 일반 내부 API 보다 read timeout 을 길게 적용한다. */
    private static SimpleClientHttpRequestFactory requestFactory(AiTagMergeProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    private record AiGenerateRequest(
            int maxCandidateCount,
            int maxGroupCount,
            int minUsageCount,
            int tagLimit
    ) {
        /** Spring API 의 nullable 검색 조건을 AI 서버가 기대하는 명시적 기본값으로 변환한다. */
        private static AiGenerateRequest from(TagMergeCandidateApiDto.GenerateRequest request) {
            return new AiGenerateRequest(
                    request.normalizedMaxCandidateCount(),
                    request.normalizedMaxGroupCount(),
                    request.normalizedMinUsageCount(),
                    request.normalizedTagLimit()
            );
        }
    }

    private record AiGenerateResponse(
            List<TagMergeCandidateApiDto.CandidateItem> items
    ) {
    }
}
