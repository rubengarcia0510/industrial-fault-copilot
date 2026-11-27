package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;

import reactor.core.publisher.Mono;

public interface DiagnosticService {

    Mono<DiagnosticResponse> diagnose(DiagnosticRequest request);
}
