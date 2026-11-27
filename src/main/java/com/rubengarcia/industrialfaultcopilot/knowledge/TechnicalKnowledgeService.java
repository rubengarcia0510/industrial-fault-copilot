package com.rubengarcia.industrialfaultcopilot.knowledge;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;

import org.springframework.stereotype.Service;

import java.util.List;

import reactor.core.publisher.Mono;

@Service
public class TechnicalKnowledgeService {

    private final TechnicalDocumentRepository repository;

    public TechnicalKnowledgeService(TechnicalDocumentRepository repository) {
        this.repository = repository;
    }

    public Mono<List<TechnicalEvidence>> search(DiagnosticRequest request) {
        return repository.search(request);
    }
}
