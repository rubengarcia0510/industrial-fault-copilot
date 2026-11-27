package com.rubengarcia.industrialfaultcopilot.ai;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiDiagnosticAdapterTests {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;

    private GeminiDiagnosticAdapter adapter;

    private final DiagnosticRequest request = new DiagnosticRequest(
            "Digital printer model X",
            "Overheating",
            "TEMP-01",
            "The warning appeared during printing"
    );

    private final TechnicalEvidence evidence = new TechnicalEvidence(
            "DOC-001",
            "Printer guide",
            "Cooling system",
            "Check the ventilation openings.",
            "manuals/printer-guide.pdf#cooling",
            "Test fixture",
            false
    );

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        adapter = new GeminiDiagnosticAdapter(chatClient, Duration.ofSeconds(2));
    }

    @Test
    void acceptsResponseWithExactEvidenceReference() {
        DiagnosticResponse response = response(
                new DiagnosticResponse.EvidenceReference(
                        "DOC-001",
                        "Cooling system",
                        "manuals/printer-guide.pdf#cooling"
                )
        );

        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenReturn(response);

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void rejectsUnknownDocumentReference() {
        DiagnosticResponse response = response(
                new DiagnosticResponse.EvidenceReference(
                        "UNKNOWN",
                        "Cooling system",
                        "manuals/printer-guide.pdf#cooling"
                )
        );

        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenReturn(response);

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectErrorMessage(
                        "Gemini returned an unknown technical evidence reference")
                .verify();
    }

    @Test
    void rejectsUnknownSectionReference() {
        DiagnosticResponse response = response(
                new DiagnosticResponse.EvidenceReference(
                        "DOC-001",
                        "Unknown section",
                        "manuals/printer-guide.pdf#cooling"
                )
        );

        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenReturn(response);

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectErrorMessage(
                        "Gemini returned an unknown technical evidence reference")
                .verify();
    }

    @Test
    void rejectsUnknownSourceLocation() {
        DiagnosticResponse response = response(
                new DiagnosticResponse.EvidenceReference(
                        "DOC-001",
                        "Cooling system",
                        "unknown-location"
                )
        );

        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenReturn(response);

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectErrorMessage(
                        "Gemini returned an unknown technical evidence reference")
                .verify();
    }

    @Test
    void sanitizesProviderErrors() {
        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenThrow(new IllegalStateException("secret-api-key-value"));

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof RuntimeException);
                    assertTrue(error.getMessage().contains(
                            "Gemini diagnostic provider failed"));
                    assertTrue(!error.getMessage().contains("secret-api-key-value"));
                })
                .verify();
    }

    @Test
    void timesOutWhenProviderDoesNotRespond() {
        when(callResponseSpec.entity(
                eq(DiagnosticResponse.class),
                any()
        )).thenAnswer(invocation -> {
            Thread.sleep(500);
            return response();
        });

        adapter = new GeminiDiagnosticAdapter(chatClient, Duration.ofMillis(50));

        StepVerifier.create(adapter.diagnose(request, List.of(evidence)))
                .expectErrorMessage("Gemini diagnostic request timed out")
                .verify(Duration.ofSeconds(3));
    }

    private DiagnosticResponse response(
            DiagnosticResponse.EvidenceReference... references
    ) {
        return new DiagnosticResponse(
                "Possible cooling issue",
                List.of("Restricted airflow"),
                List.of("Inspect external ventilation"),
                List.of(references),
                "Requires technician confirmation",
                false
        );
    }
}
