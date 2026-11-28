package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import com.rubengarcia.industrialfaultcopilot.knowledge.TechnicalEvidence;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DiagnosticProvider {

    Mono<DiagnosticResponse> diagnose(
            DiagnosticRequest request,
            List<TechnicalEvidence> evidence
    );
}
