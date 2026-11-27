package com.rubengarcia.industrialfaultcopilot;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticService;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class DiagnosticServiceTestConfiguration {

    @Bean
    DiagnosticService diagnosticService() {
        return mock(DiagnosticService.class);
    }
}
