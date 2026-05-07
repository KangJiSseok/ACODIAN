package com.ibank.axwms.global.config;

import com.ibank.axwms.domain.worklog.external.AiWorklogSearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API 서버가 내부 AI 서비스와 통신할 때 필요한 설정 프로퍼티를 등록한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiWorklogSearchProperties.class)
public class AiIntegrationConfig {
}
