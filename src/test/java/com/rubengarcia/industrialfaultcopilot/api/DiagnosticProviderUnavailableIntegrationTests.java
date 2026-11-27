package com.rubengarcia.industrialfaultcopilot.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DiagnosticProviderUnavailableIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returnsServiceUnavailableWhenDiagnosticServiceIsNotConfigured() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Paper feed interruption",
                          "alarmCode": "PAPER-02"
                        }
                        """)
                .exchange()
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
}
