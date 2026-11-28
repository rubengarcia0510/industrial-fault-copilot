package com.rubengarcia.industrialfaultcopilot.knowledge;

public record TechnicalEvidence(
        String documentId,
        String title,
        String section,
        String excerpt,
        String sourceLocation,
        String provenance,
        boolean synthetic
) {
}
