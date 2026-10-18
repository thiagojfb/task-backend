package com.task.task_backend.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio único da aplicação no fuso de Brasília; substituído por {@code Clock.fixed} nos testes. */
@Configuration
public class RelogioConfig {

    public static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO_BRASILIA);
    }
}
