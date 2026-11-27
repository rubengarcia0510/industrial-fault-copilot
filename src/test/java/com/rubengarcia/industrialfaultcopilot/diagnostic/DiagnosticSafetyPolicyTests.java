package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSafetyPolicyTests {

    private DiagnosticSafetyPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DiagnosticSafetyPolicy();
    }

    @Test
    void requiresEscalationForElectricalRisk() {
        DiagnosticRequest request = request(
                "The operator reports an electric shock near the machine"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void requiresEscalationForThermalRisk() {
        DiagnosticRequest request = request(
                "There is smoke and a burning smell"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void requiresEscalationForPressureRisk() {
        DiagnosticRequest request = request(
                "The pneumatic system reports abnormal pressure"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void requiresEscalationForInterlockRisk() {
        DiagnosticRequest request = request(
                "The safety interlock is not working"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void requiresEscalationForMechanicalRisk() {
        DiagnosticRequest request = request(
                "A moving part is causing an unexpected noise"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void detectsSpanishRiskDescriptions() {
        DiagnosticRequest request = request(
                "Hay humo y olor a quemado cerca del tablero"
        );

        assertTrue(policy.requiresEscalation(request));
    }

    @Test
    void doesNotEscalateOrdinaryNonRiskSymptoms() {
        DiagnosticRequest request = request(
                "The printed image has horizontal color bands"
        );

        assertFalse(policy.requiresEscalation(request));
    }

    @Test
    void escalatesWhenRequestIsNull() {
        assertTrue(policy.requiresEscalation(null));
    }

    private DiagnosticRequest request(String observations) {
        return new DiagnosticRequest(
                "Digital printer",
                "Unexpected behavior",
                null,
                observations
        );
    }
}
