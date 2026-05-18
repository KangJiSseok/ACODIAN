package com.ibank.axwms.global.config;

import com.ibank.axwms.domain.file.external.S3StorageProperties;
import com.ibank.axwms.domain.organization.user.scheduler.ProfileImageCleanupProperties;
import com.ibank.axwms.domain.worklog.config.WorklogFileProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        S3StorageProperties.class,
        ProfileImageCleanupProperties.class,
        WorklogFileProperties.class
})
public class StorageConfig {

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
                ))
                .build();
    }
}
