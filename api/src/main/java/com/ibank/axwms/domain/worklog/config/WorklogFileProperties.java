package com.ibank.axwms.domain.worklog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 업무일지 첨부 파일 개수와 용량 상한을 설정으로 분리한다.
 * 서버 전역 multipart 하드 리밋과 별도로, 업무일지 도메인 정책을 서비스 계층에서 일관되게 적용한다.
 */
@ConfigurationProperties(prefix = "worklog.files")
public record WorklogFileProperties(
        int maxCount,
        long maxFileSizeBytes,
        long maxTotalSizeBytes
) {
}
