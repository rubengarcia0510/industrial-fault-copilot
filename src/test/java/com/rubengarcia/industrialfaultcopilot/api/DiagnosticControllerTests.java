package com.rubengarcia.industrialfaultcopilot.api;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticService;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(DiagnosticController.class)
class DiagnosticControllerTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DiagnosticService diagnosticService;

    @Test
    void returnsDiagnosticResponseForValidRequest() {
        DiagnosticResponse response = new DiagnosticResponse(
                "Possible cooling system issue",
                List.of("Restricted airflow"),
                List.of("Check external ventilation openings"),
                List.of(new DiagnosticResponse.EvidenceReference(
                        "printer-guide.pdf",
                        "Cooling system"
                )),
                "The available evidence is limited",
                false
        );

        when(diagnosticService.diagnose(any(DiagnosticRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Overheating",
                          "alarmCode": "TEMP-01",
                          "observations": "The warning appeared during printing"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.summary").isEqualTo("Possible cooling system issue")
                .jsonPath("$.hypotheses[0]").isEqualTo("Restricted airflow")
                .jsonPath("$.safeChecks[0]").isEqualTo("Check external ventilation openings")
                .jsonPath("$.evidenceReferences[0].document").isEqualTo("printer-guide.pdf")
                .jsonPath("$.evidenceReferences[0].section").isEqualTo("Cooling system")
                .jsonPath("$.uncertainty").isEqualTo("The available evidence is limited")
                .jsonPath("$.escalationRequired").isEqualTo(false);

        verify(diagnosticService).diagnose(new DiagnosticRequest(
                "Digital printer model X",
                "Overheating",
                "TEMP-01",
                "The warning appeared during printing"
        ));
    }

    @Test
    void rejectsRequestWhenMachineIsMissing() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "symptom": "Overheating"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsRequestWhenMachineIsBlank() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": " ",
                          "symptom": "Overheating"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsRequestWhenSymptomIsMissing() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsRequestWhenSymptomIsBlank() {
        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": " "
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void acceptsOptionalFieldsWhenTheyAreOmitted() {
        when(diagnosticService.diagnose(any(DiagnosticRequest.class)))
                .thenReturn(Mono.just(new DiagnosticResponse(
                        "Initial assessment",
                        List.of(),
                        List.of(),
                        List.of(),
                        "Insufficient information",
                        true
                )));

        webTestClient.post()
                .uri("/api/v1/diagnostics")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "machine": "Digital printer model X",
                          "symptom": "Paper feed issue"
                        }
                        """)
                .exchange()
                .expectStatus().isOk();

        verify(diagnosticService).diagnose(new DiagnosticRequest(
                "Digital printer model X",
                "Paper feed issue",
                null,
                null
        ));
    }
}
