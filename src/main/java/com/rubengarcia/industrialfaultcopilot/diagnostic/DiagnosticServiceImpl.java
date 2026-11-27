package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalKnowledgeService;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

public class DiagnosticServiceImpl implements DiagnosticService {

    private static final String SAFE_ESCALATION =
            "The diagnostic provider could not complete a reliable assessment. Human review is required.";

    private static final Pattern UNSAFE_RECOMMENDATION = Pattern.compile(
            "\\b(open|remove|bypass|disable|override|bridge|short|touch|repair|replace|adjust|modify|"
                    + "energized|live wire|electrical panel|control cabinet|"
                    + "abrir|retirar|puentear|desactivar|anular|tocar|reparar|reemplazar|"
                    + "ajustar|modificar|tablero electrico|componente energizado)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final TechnicalKnowledgeService knowledgeService;
    private final DiagnosticProvider diagnosticProvider;
    private final DiagnosticSafetyPolicy safetyPolicy;

    public DiagnosticServiceImpl(
            TechnicalKnowledgeService knowledgeService,
            DiagnosticProvider diagnosticProvider,
            DiagnosticSafetyPolicy safetyPolicy
    ) {
        this.knowledgeService = knowledgeService;
        this.diagnosticProvider = diagnosticProvider;
        this.safetyPolicy = safetyPolicy;
    }

    @Override
    public Mono<DiagnosticResponse> diagnose(DiagnosticRequest request) {
        if (safetyPolicy.requiresEscalation(request)) {
            return Mono.just(escalation(
                    "A safety risk was detected in the reported information. "
                            + "Stop troubleshooting and escalate to qualified personnel."
            ));
        }

        return Mono.defer(() -> knowledgeService.search(request))
                .flatMap(evidence -> {
                    if (evidence == null || evidence.isEmpty()) {
                        return Mono.just(insufficientEvidence());
                    }

                    return Mono.defer(() -> diagnosticProvider.diagnose(request, evidence))
                            .map(this::enforceSafeRecommendations)
                            .switchIfEmpty(Mono.just(escalation(SAFE_ESCALATION)))
                            .onErrorResume(error ->
                                    Mono.just(escalation(SAFE_ESCALATION)));
                })
                .switchIfEmpty(Mono.just(insufficientEvidence()))
                .onErrorResume(error ->
                        Mono.just(escalation(SAFE_ESCALATION)));
    }

    private DiagnosticResponse enforceSafeRecommendations(DiagnosticResponse response) {
        if (response == null) {
            return escalation(SAFE_ESCALATION);
        }

        List<String> safeChecks = response.safeChecks() == null
                ? List.of()
                : response.safeChecks();

        boolean containsUnsafeRecommendation = safeChecks.stream()
                .filter(check -> check != null)
                .anyMatch(check -> UNSAFE_RECOMMENDATION.matcher(check).find());

        if (containsUnsafeRecommendation) {
            return new DiagnosticResponse(
                    response.summary(),
                    response.hypotheses(),
                    List.of(),
                    response.evidenceReferences(),
                    "A potentially unsafe recommendation was omitted. Human review is required.",
                    true
            );
        }

        return response;
    }

    private DiagnosticResponse insufficientEvidence() {
        return new DiagnosticResponse(
                "Insufficient technical evidence",
                List.of(),
                List.of(),
                List.of(),
                "No matching technical evidence was found. Human review is required.",
                true
        );
    }

    private DiagnosticResponse escalation(String reason) {
        return new DiagnosticResponse(
                "Diagnostic escalation required",
                List.of(),
                List.of(),
                List.of(),
                reason,
                true
        );
    }
}
