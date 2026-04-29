package com.ibank.axwms.domain.file.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        String basePrefix,
        String publicBaseUrl,
        String accessKeyId,
        String secretAccessKey
) {
}
