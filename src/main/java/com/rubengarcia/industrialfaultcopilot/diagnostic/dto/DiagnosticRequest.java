package com.rubengarcia.industrialfaultcopilot.diagnostic.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticRequest(
        @NotBlank String machine,
        @NotBlank String symptom,
        String alarmCode,
        String observations
) {
}
