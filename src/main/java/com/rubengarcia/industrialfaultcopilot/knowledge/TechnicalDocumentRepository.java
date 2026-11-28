package com.rubengarcia.industrialfaultcopilot.knowledge;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import java.util.List;

import reactor.core.publisher.Mono;

public interface TechnicalDocumentRepository {

    Mono<List<TechnicalEvidence>> search(DiagnosticRequest request);
}
