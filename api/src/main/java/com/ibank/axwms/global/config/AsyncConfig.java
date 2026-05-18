package com.ibank.axwms.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 커밋 이후 후처리 listener 가 HTTP 응답 스레드와 분리되어 실행되도록 Spring 비동기 처리를 활성화한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {
}
