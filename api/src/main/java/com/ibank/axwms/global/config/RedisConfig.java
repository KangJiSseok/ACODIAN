package com.ibank.axwms.global.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis 인프라 설정 자리. RefreshToken 저장은 Spring Data Redis 의 기본 ConnectionFactory/RedisTemplate 으로 처리되며,
 * 이후 커스텀 직렬화나 별도 RedisTemplate 빈이 필요해지면 이 클래스에 모아 등록한다.
 */
@Configuration(proxyBeanMethods = false)
public class RedisConfig {
}
