package com.rubengarcia.industrialfaultcopilot.diagnostic.dto;

import java.util.List;

public record DiagnosticResponse(
    String summary,
    List<String> hypotheses,
    List<String> safeChecks,
    List<EvidenceReference> evidenceReferences,
    String uncertainty,
    boolean escalationRequired
) {
    public record EvidenceReference(
        String document,
        String section,
        String sourceLocation
    ) {}
}
