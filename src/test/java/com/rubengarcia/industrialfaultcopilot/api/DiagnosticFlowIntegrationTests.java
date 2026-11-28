package com.rubengarcia.industrialfaultcopilot.api;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticProvider;
import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticSafetyPolicy;
import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticService;
import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticServiceImpl;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalKnowledgeService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(DiagnosticFlowIntegrationTests.TestConfig.class)
class DiagnosticFlowIntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DiagnosticProvider diagnosticProvider;

    @Test
    void returnsStructuredDiagnosisWithMatchingEvidenceAndAlarmCode() {
        DiagnosticResponse providerResponse = new DiagnosticResponse(
                "Paper feeding interruption detected",
                List.of("Paper positioning may be incorrect"),
                List.of("Review the externally visible paper status indicators"),
                List.of(new DiagnosticResponse.EvidenceReference(
                        "SYN-PRINTER-003",
                        "Paper transport",
                        "synthetic://printer-troubleshooting/paper-feed#transport"
                )),
                "The assessment is based on matching synthetic technical evidence.",
                false
        );

        when(diagnosticProvider.diagnose(any(), anyList()))
                .thenReturn(Mono.just(providerResponse));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Paper feed interruption",
                          "alarmCode": "PAPER-02",
                          "observations": "The issue occurs during normal printing"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Paper feeding interruption detected")
                .jsonPath("$.hypotheses[0]").isEqualTo("Paper positioning may be incorrect")
                .jsonPath("$.safeChecks[0]").isEqualTo(
                        "Review the externally visible paper status indicators"
                )
                .jsonPath("$.evidenceReferences[0].document").isEqualTo("SYN-PRINTER-003")
                .jsonPath("$.evidenceReferences[0].section").isEqualTo("Paper transport")
                .jsonPath("$.evidenceReferences[0].sourceLocation")
                .isEqualTo("synthetic://printer-troubleshooting/paper-feed#transport")
                .jsonPath("$.escalationRequired").isEqualTo(false);

        verify(diagnosticProvider).diagnose(any(), anyList());
    }

    @Test
    void escalatesWithoutInvokingProviderWhenAlarmCodeHasNoMatchingEvidence() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Paper feed interruption",
                          "alarmCode": "PAPER-99",
                          "observations": "The issue occurs during normal printing"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Insufficient technical evidence")
                .jsonPath("$.evidenceReferences").isEmpty()
                .jsonPath("$.escalationRequired").isEqualTo(true);

        verify(diagnosticProvider, never()).diagnose(any(), anyList());
    }

    @Test
    void escalatesWhenProviderReportsAmbiguousOrConflictingEvidence() {
        DiagnosticResponse ambiguousResponse = new DiagnosticResponse(
                "Diagnostic escalation required",
                List.of(),
                List.of(),
                List.of(),
                "The available information is ambiguous or conflicting.",
                true
        );

        when(diagnosticProvider.diagnose(any(), anyList()))
                .thenReturn(Mono.just(ambiguousResponse));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(validPaperFeedRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Diagnostic escalation required")
                .jsonPath("$.uncertainty")
                .isEqualTo("The available information is ambiguous or conflicting.")
                .jsonPath("$.escalationRequired").isEqualTo(true);
    }

    @Test
    void escalatesSafetyRiskBeforeInvokingProvider() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Electrical failure",
                          "alarmCode": "ELEC-01",
                          "observations": "Smoke near the electrical panel"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Diagnostic escalation required")
                .jsonPath("$.escalationRequired").isEqualTo(true)
                .jsonPath("$.uncertainty").value(
                        value -> org.assertj.core.api.Assertions.assertThat(value.toString())
                                .contains("safety risk")
                );

        verify(diagnosticProvider, never()).diagnose(any(), anyList());
    }

    @Test
    void escalatesWhenProviderFails() {
        when(diagnosticProvider.diagnose(any(), anyList()))
                .thenReturn(Mono.error(new IllegalStateException("Provider unavailable")));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(validPaperFeedRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Diagnostic escalation required")
                .jsonPath("$.safeChecks").isEmpty()
                .jsonPath("$.escalationRequired").isEqualTo(true);
    }

    @Test
    void escalatesWhenProviderTimesOut() {
        when(diagnosticProvider.diagnose(any(), anyList()))
                .thenReturn(Mono.error(new TimeoutException("Provider timed out")));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(validPaperFeedRequest())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Diagnostic escalation required")
                .jsonPath("$.safeChecks").isEmpty()
                .jsonPath("$.escalationRequired").isEqualTo(true);
    }

    @Test
    void rejectsInvalidRequestWithBadRequest() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": " ",
                          "symptom": "",
                          "alarmCode": "PAPER-02"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        verify(diagnosticProvider, never()).diagnose(any(), anyList());
    }

    private String validPaperFeedRequest() {
        return """
                {
                  "machine": "Digital printer model X",
                  "symptom": "Paper feed interruption",
                  "alarmCode": "PAPER-02",
                  "observations": "The issue occurs during normal printing"
                }
                """;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        DiagnosticService diagnosticService(
                TechnicalKnowledgeService knowledgeService,
                DiagnosticProvider diagnosticProvider,
                DiagnosticSafetyPolicy safetyPolicy
        ) {
            return new DiagnosticServiceImpl(
                    knowledgeService,
                    diagnosticProvider,
                    safetyPolicy
            );
        }
    }
}
