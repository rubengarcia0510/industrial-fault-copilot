package com.rubengarcia.industrialfaultcopilot.diagnostic;

import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DiagnosticService {

    public Mono<DiagnosticResponse> diagnose(DiagnosticRequest request) {
        return Mono.just(new DiagnosticResponse(
                "Diagnostic pending agent integration",
                List.of(),
                List.of(),
                true
        ));
    }
}
