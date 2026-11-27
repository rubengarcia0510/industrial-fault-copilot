package com.rubengarcia.industrialfaultcopilot.knowledge;

import tools.jackson.databind.ObjectMapper;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathTechnicalDocumentRepositoryTests {

    private ClasspathTechnicalDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ClasspathTechnicalDocumentRepository(
                new PathMatchingResourcePatternResolver(),
                new ObjectMapper()
        );
    }

    @Test
    void findsDocumentsMatchingMachineAndSymptom() {
        verifySearch("HP Indigo digital press", "The printer is overheating", null,
                results -> {
                    assertEquals(2, results.size());
                    assertEquals(
                            List.of("SYN-PRINTER-001", "SYN-PRINTER-002"),
                            results.stream().map(TechnicalEvidence::documentId).toList()
                    );
                });
    }

    @Test
    void requiresExactAlarmCodeMatch() {
        verifySearch("HP Indigo", "overheating", "TEMP-010",
                results -> assertTrue(results.isEmpty()));
    }

    @Test
    void matchesAlarmCodeIgnoringCaseAndSurroundingWhitespace() {
        verifySearch("HP Indigo", "overheating", " temp-01 ",
                results -> assertEquals(2, results.size()));
    }

    @Test
    void returnsEmptyListWhenMachineDoesNotMatch() {
        verifySearch("unrelated machine", "overheating", null,
                results -> assertTrue(results.isEmpty()));
    }

    @Test
    void returnsSingleResultForPaperFeedProblem() {
        verifySearch("HP Indigo", "paper feed problem", "PAPER-02",
                results -> {
                    assertEquals(1, results.size());
                    assertEquals("SYN-PRINTER-003", results.getFirst().documentId());
                });
    }

    @Test
    void preservesEvidenceMetadataAndSyntheticProvenance() {
        verifySearch("HP Indigo", "overheating", "TEMP-01",
                results -> {
                    TechnicalEvidence evidence = results.getFirst();

                    assertEquals("SYN-PRINTER-001", evidence.documentId());
                    assertEquals("Thermal system troubleshooting", evidence.title());
                    assertEquals("Temperature monitoring", evidence.section());
                    assertFalse(evidence.excerpt().isBlank());
                    assertTrue(evidence.sourceLocation().startsWith("synthetic://"));
                    assertEquals(
                            "Synthetic training example; not manufacturer documentation.",
                            evidence.provenance()
                    );
                    assertTrue(evidence.synthetic());
                });
    }

    private void verifySearch(
            String machine,
            String symptom,
            String alarmCode,
            java.util.function.Consumer<List<TechnicalEvidence>> assertions
    ) {
        DiagnosticRequest request = new DiagnosticRequest(
                machine,
                symptom,
                alarmCode,
                null
        );

        StepVerifier.create(repository.search(request))
                .assertNext(assertions::accept)
                .verifyComplete();
    }
}
