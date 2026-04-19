package com.ibank.axwms.global.jooq;

import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.RenderNameCase;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {

    @Bean
    DefaultConfigurationCustomizer jooqDefaultConfigurationCustomizer() {
        return configuration -> configuration.settings()
                .withRenderQuotedNames(RenderQuotedNames.NEVER)
                .withRenderNameCase(RenderNameCase.LOWER_IF_UNQUOTED);
    }
}
