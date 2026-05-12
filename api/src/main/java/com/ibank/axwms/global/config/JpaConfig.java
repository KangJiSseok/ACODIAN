package com.ibank.axwms.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

//JpaConfig를 CGLIB로 감싸지 않음 (더 가벼워짐)
@Configuration(proxyBeanMethods = false)
//EntityManagerFactory 빈이 있을때만 이 설정 클래스를 활성화 ()
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaAuditing
public class JpaConfig {
}
