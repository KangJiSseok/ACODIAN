package com.ibank.axwms.domain.worklog.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 업무일지 등록 후 AI 서버의 본문 요약/태그 파이프라인을 호출할 때 쓰는 설정값이다.
 */
@ConfigurationProperties(prefix = "ai.worklog-pipeline")
public record AiWorklogPipelineProperties(
        boolean enabled,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    private static final String DEFAULT_BASE_URL = "http://localhost:8000/ai";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

    public AiWorklogPipelineProperties {
        baseUrl = normalizeBaseUrl(baseUrl);
        connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    }

    /** 누락된 baseUrl 은 로컬 AI 서버 기본 prefix 로 대체하고 trailing slash 를 제거한다. */
    private static String normalizeBaseUrl(String value) {
        String candidate = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.trim();
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }
}
