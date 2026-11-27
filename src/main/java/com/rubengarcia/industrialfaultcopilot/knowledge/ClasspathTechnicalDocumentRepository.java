package com.rubengarcia.industrialfaultcopilot.knowledge;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Repository
public class ClasspathTechnicalDocumentRepository
        implements TechnicalDocumentRepository {

    private static final String DOCUMENTS_PATTERN = "classpath*:knowledge/*.json";

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    public ClasspathTechnicalDocumentRepository(
            ResourcePatternResolver resourcePatternResolver,
            ObjectMapper objectMapper
    ) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<List<TechnicalEvidence>> search(DiagnosticRequest request) {
        return Mono.fromCallable(() -> loadDocuments().stream()
                .filter(document -> matchesMachine(document, request.machine()))
                .filter(document -> matchesSymptom(document, request.symptom()))
                .filter(document -> matchesAlarmCode(document, request.alarmCode()))
                .map(TechnicalDocument::toEvidence)
                .sorted(Comparator.comparing(TechnicalEvidence::documentId))
                .toList());
    }

    private List<TechnicalDocument> loadDocuments() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(DOCUMENTS_PATTERN);

        return Arrays.stream(resources)
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(this::readDocuments)
                .flatMap(List::stream)
                .toList();
    }

    private List<TechnicalDocument> readDocuments(Resource resource) {
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<TechnicalDocument>>() {
                    }
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load technical knowledge resource: "
                            + resource.getDescription(),
                    exception
            );
        }
    }

    private boolean matchesMachine(TechnicalDocument document, String machine) {
        String normalizedMachine = normalize(machine);

        return document.machineKeywords().stream()
                .map(ClasspathTechnicalDocumentRepository::normalize)
                .anyMatch(normalizedMachine::contains);
    }

    private boolean matchesSymptom(TechnicalDocument document, String symptom) {
        String normalizedSymptom = normalize(symptom);

        return document.symptomKeywords().stream()
                .map(ClasspathTechnicalDocumentRepository::normalize)
                .anyMatch(normalizedSymptom::contains);
    }

    private boolean matchesAlarmCode(TechnicalDocument document, String alarmCode) {
        if (alarmCode == null || alarmCode.isBlank()) {
            return true;
        }

        String requestedCode = alarmCode.trim().toUpperCase(Locale.ROOT);

        return document.alarmCodes().stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch(requestedCode::equals);
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private record TechnicalDocument(
            String documentId,
            String title,
            String section,
            String excerpt,
            String sourceLocation,
            String provenance,
            boolean synthetic,
            List<String> machineKeywords,
            List<String> symptomKeywords,
            List<String> alarmCodes
    ) {

        private TechnicalEvidence toEvidence() {
            return new TechnicalEvidence(
                    documentId,
                    title,
                    section,
                    excerpt,
                    sourceLocation,
                    provenance,
                    synthetic
            );
        }
    }
}
