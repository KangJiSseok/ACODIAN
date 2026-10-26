package com.ibank.axwms.domain.worklog.external;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** GraphRAG 비동기 색인 접수 경로의 내부 AI 연결 설정이다. */
@ConfigurationProperties(prefix = "ai.worklog-graph-index")
public record AiWorklogGraphIndexProperties(
        boolean enabled,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public AiWorklogGraphIndexProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8000/ai" : baseUrl.strip();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(3) : readTimeout;
    }
}
