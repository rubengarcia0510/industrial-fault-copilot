package com.rubengarcia.industrialfaultcopilot.knowledge;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalKnowledgeServiceTests {

    @Mock
    private TechnicalDocumentRepository repository;

    @InjectMocks
    private TechnicalKnowledgeService service;

    @Test
    void delegatesSearchAndReturnsRepositoryResults() {
        DiagnosticRequest request = new DiagnosticRequest(
                "HP Indigo",
                "overheating",
                "TEMP-01",
                null
        );

        List<TechnicalEvidence> expected = List.of(
                new TechnicalEvidence(
                        "SYN-001",
                        "Thermal troubleshooting",
                        "Temperature monitoring",
                        "Inspect ventilation.",
                        "synthetic://thermal",
                        "Synthetic training example.",
                        true
                )
        );

        when(repository.search(request)).thenReturn(Mono.just(expected));

        StepVerifier.create(service.search(request))
                .expectNext(expected)
                .verifyComplete();

        verify(repository).search(request);
    }
}
