package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalEvidence;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalKnowledgeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticServiceImplTests {

    @Mock
    private TechnicalKnowledgeService knowledgeService;

    @Mock
    private DiagnosticProvider diagnosticProvider;

    @InjectMocks
    private DiagnosticServiceImpl diagnosticService;

    private DiagnosticSafetyPolicy safetyPolicy;

    private DiagnosticRequest request;
    private TechnicalEvidence evidence;

    @BeforeEach
    void setUp() {
        safetyPolicy = new DiagnosticSafetyPolicy();
        diagnosticService = new DiagnosticServiceImpl(
                knowledgeService,
                diagnosticProvider,
                safetyPolicy
        );

        request = new DiagnosticRequest(
                "Digital printer model X",
                "Horizontal color bands",
                "PRINT-04",
                "The issue appears during normal printing"
        );

        evidence = new TechnicalEvidence(
                "printer-manual-01",
                "Printer technical manual",
                "Print quality",
                "Horizontal bands may be associated with printhead calibration.",
                "manuals/printer-manual.pdf#print-quality",
                "official-manual",
                false
        );
    }

    @Test
    void doesNotInvokeProviderWhenNoEvidenceIsFound() {
        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of()));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertEquals("Insufficient technical evidence", response.summary());
        assertTrue(response.evidenceReferences().isEmpty());

        verify(diagnosticProvider, never()).diagnose(any(), any());
    }

    @Test
    void doesNotInvokeProviderWhenSafetyRiskIsDetected() {
        DiagnosticRequest riskyRequest = new DiagnosticRequest(
                "Digital printer model X",
                "Electrical failure",
                null,
                "The operator reports smoke near the electrical panel"
        );

        DiagnosticResponse response = diagnosticService.diagnose(riskyRequest).block();

        assertTrue(response.escalationRequired());
        assertTrue(response.uncertainty().contains("safety risk"));

        verify(knowledgeService, never()).search(any());
        verify(diagnosticProvider, never()).diagnose(any(), any());
    }

    @Test
    void passesRetrievedEvidenceToProvider() {
        DiagnosticResponse expected = new DiagnosticResponse(
                "Possible printhead calibration issue",
                List.of("Printhead calibration may be required"),
                List.of("Review the externally visible print quality settings"),
                List.of(new DiagnosticResponse.EvidenceReference(
                        "printer-manual-01",
                        "Print quality",
                        "manuals/printer-manual.pdf#print-quality"
                )),
                "The assessment is based on matching manual evidence.",
                false
        );

        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of(evidence)));
        when(diagnosticProvider.diagnose(request, List.of(evidence)))
                .thenReturn(Mono.just(expected));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertEquals(expected, response);
        assertEquals(
                "manuals/printer-manual.pdf#print-quality",
                response.evidenceReferences().get(0).sourceLocation()
        );

        verify(diagnosticProvider).diagnose(request, List.of(evidence));
    }

    @Test
    void requiresEscalationWhenProviderReturnsAnEscalation() {
        DiagnosticResponse providerResponse = new DiagnosticResponse(
                "Uncertain assessment",
                List.of(),
                List.of(),
                List.of(),
                "The available information is ambiguous.",
                true
        );

        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of(evidence)));
        when(diagnosticProvider.diagnose(request, List.of(evidence)))
                .thenReturn(Mono.just(providerResponse));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertEquals(providerResponse.uncertainty(), response.uncertainty());
    }

    @Test
    void escalatesWhenProviderFails() {
        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of(evidence)));
        when(diagnosticProvider.diagnose(request, List.of(evidence)))
                .thenReturn(Mono.error(new RuntimeException("Provider unavailable")));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertEquals("Diagnostic escalation required", response.summary());
        assertTrue(response.safeChecks().isEmpty());
        assertTrue(response.uncertainty().contains("could not complete"));
    }

    @Test
    void escalatesWhenProviderReturnsAnEmptyResult() {
        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of(evidence)));
        when(diagnosticProvider.diagnose(request, List.of(evidence)))
                .thenReturn(Mono.empty());

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertEquals("Diagnostic escalation required", response.summary());
    }

    @Test
    void removesUnsafeRecommendationsAndRequiresEscalation() {
        DiagnosticResponse providerResponse = new DiagnosticResponse(
                "Possible maintenance issue",
                List.of("A component may be obstructed"),
                List.of(
                        "Check the externally visible status indicator",
                        "Open the electrical panel and inspect the wiring"
                ),
                List.of(),
                "The assessment is preliminary.",
                false
        );

        when(knowledgeService.search(request)).thenReturn(Mono.just(List.of(evidence)));
        when(diagnosticProvider.diagnose(request, List.of(evidence)))
                .thenReturn(Mono.just(providerResponse));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertTrue(response.safeChecks().isEmpty());
        assertTrue(response.uncertainty().contains("unsafe recommendation"));
    }

    @Test
    void escalatesWhenKnowledgeSearchFails() {
        when(knowledgeService.search(request))
                .thenReturn(Mono.error(new RuntimeException("Knowledge repository unavailable")));

        DiagnosticResponse response = diagnosticService.diagnose(request).block();

        assertTrue(response.escalationRequired());
        assertEquals("Diagnostic escalation required", response.summary());

        verify(diagnosticProvider, never()).diagnose(any(), any());
    }
}
