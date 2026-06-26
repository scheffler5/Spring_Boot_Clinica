package com.learn.projeto_learn.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer baselineExistingSchemas() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("0");
    }
}
