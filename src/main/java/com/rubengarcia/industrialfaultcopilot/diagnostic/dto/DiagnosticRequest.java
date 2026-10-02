package com.rubengarcia.industrialfaultcopilot.diagnostic.dto;

public record DiagnosticRequest(
        String machine,
        String symptom,
        String alarmCode
) {
}
