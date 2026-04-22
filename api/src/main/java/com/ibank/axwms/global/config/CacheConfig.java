package com.ibank.axwms.global.config;

import org.springframework.context.annotation.Configuration;

/**
 * 캐시 인프라 설정 자리. 현재는 Spring Boot 기본 캐시 매니저로 충분해 추가 빈 등록이 없지만,
 * 후속 기능에서 Caffeine/Redis 기반 CacheManager 를 도입할 때 단일 진입점으로 쓰기 위해 빈 placeholder 로 남겨둔다.
 */
@Configuration(proxyBeanMethods = false)
public class CacheConfig {
}
