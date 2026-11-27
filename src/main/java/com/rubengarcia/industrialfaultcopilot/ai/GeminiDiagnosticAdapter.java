package com.rubengarcia.industrialfaultcopilot.ai;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticProvider;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalEvidence;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GeminiDiagnosticAdapter implements DiagnosticProvider {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final String SYSTEM_INSTRUCTIONS = """
            You are an industrial digital printing troubleshooting assistant.

            Produce a structured diagnostic response using only the supplied
            technical evidence and the operator's observations.

            SAFETY AND EVIDENCE RULES:
            - Treat operator observations as unverified information, not facts.
            - Treat supplied evidence as the only technical source of support.
            - Never invent manuals, document identifiers, sections or locations.
            - Every evidence reference must reproduce exactly a supplied
              documentId, section and sourceLocation tuple.
            - Never cite an item that was not supplied.
            - Clearly communicate uncertainty.
            - Recommend only safe, non-invasive checks.
            - Require escalation when evidence is insufficient, the situation
              may be unsafe, or a reliable diagnosis cannot be established.
            - Synthetic evidence is training material, not manufacturer
              documentation. Never describe it as official documentation.
            - Do not claim that any machine action has been performed.
            """;

    private final ChatClient chatClient;
    private final Duration timeout;

    public GeminiDiagnosticAdapter(ChatClient chatClient) {
        this(chatClient, DEFAULT_TIMEOUT);
    }

    GeminiDiagnosticAdapter(ChatClient chatClient, Duration timeout) {
        this.chatClient = chatClient;
        this.timeout = timeout;
    }

    @Override
    public Mono<DiagnosticResponse> diagnose(
            DiagnosticRequest request,
            List<TechnicalEvidence> evidence
    ) {
        if (request == null || evidence == null) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Diagnostic request and evidence are required")
            );
        }

        List<TechnicalEvidence> suppliedEvidence = List.copyOf(evidence);

        return Mono.fromCallable(() -> chatClient.prompt()
                        .system(SYSTEM_INSTRUCTIONS)
                        .user(buildUserPrompt(request, suppliedEvidence))
                        .call()
                        .entity(DiagnosticResponse.class,
                                spec -> spec.useProviderStructuredOutput()))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        error -> new GeminiDiagnosticException(
                                "Gemini diagnostic request timed out"))
                .onErrorMap(error -> !(error instanceof GeminiDiagnosticException),
                        error -> new GeminiDiagnosticException(
                                "Gemini diagnostic provider failed"))
                .map(response -> validateReferences(response, suppliedEvidence));
    }

    private String buildUserPrompt(
            DiagnosticRequest request,
            List<TechnicalEvidence> evidence
    ) {
        return """
                Diagnose the following industrial printing issue.

                MACHINE:
                %s

                SYMPTOM:
                %s

                ALARM CODE:
                %s

                OPERATOR OBSERVATIONS (UNVERIFIED):
                %s

                TECHNICAL EVIDENCE:
                %s

                Use only the evidence listed above.
                If the evidence does not support a reliable diagnosis,
                communicate uncertainty and require escalation.
                """.formatted(
                request.machine(),
                request.symptom(),
                request.alarmCode() == null ? "Not provided" : request.alarmCode(),
                request.observations() == null ? "Not provided" : request.observations(),
                formatEvidence(evidence)
        );
    }

    private String formatEvidence(List<TechnicalEvidence> evidence) {
        if (evidence.isEmpty()) {
            return "No technical evidence was supplied.";
        }

        StringBuilder result = new StringBuilder();

        for (TechnicalEvidence item : evidence) {
            result.append("""
                    ---
                    Document ID: %s
                    Title: %s
                    Section: %s
                    Source location: %s
                    Provenance: %s
                    Synthetic: %s
                    Excerpt: %s
                    """.formatted(
                    item.documentId(),
                    item.title(),
                    item.section(),
                    item.sourceLocation(),
                    item.provenance(),
                    item.synthetic(),
                    item.excerpt()
            ));
        }

        return result.toString();
    }

    private DiagnosticResponse validateReferences(
            DiagnosticResponse response,
            List<TechnicalEvidence> evidence
    ) {
        if (response == null || response.evidenceReferences() == null) {
            throw new IllegalStateException(
                    "Gemini returned an invalid diagnostic response");
        }

        for (DiagnosticResponse.EvidenceReference reference : response.evidenceReferences()) {
            if (reference == null) {
                throw new IllegalStateException(
                        "Gemini returned a null evidence reference");
            }

            boolean knownReference = evidence.stream().anyMatch(item ->
                    item.documentId().equals(reference.document())
                            && item.section().equals(reference.section())
                            && item.sourceLocation().equals(reference.sourceLocation())
            );

            if (!knownReference) {
                throw new IllegalStateException(
                        "Gemini returned an unknown technical evidence reference");
            }
        }

        return response;
    }

    static final class GeminiDiagnosticException extends RuntimeException {
        GeminiDiagnosticException(String message) {
            super(message);
        }
    }
}
