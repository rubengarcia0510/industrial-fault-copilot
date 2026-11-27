package com.rubengarcia.industrialfaultcopilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(DiagnosticServiceTestConfiguration.class)
class IndustrialFaultCopilotApplicationTests {

    @Test
    void contextLoads() {
    }
}
