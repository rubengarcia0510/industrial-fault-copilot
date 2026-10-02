package com.rubengarcia.industrialfaultcopilot.api;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticService;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticController {

    private final DiagnosticService diagnosticService;

    public DiagnosticController(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @PostMapping
    public Mono<DiagnosticResponse> diagnose(
            @RequestBody DiagnosticRequest request) {
        return diagnosticService.diagnose(request);
    }
}
