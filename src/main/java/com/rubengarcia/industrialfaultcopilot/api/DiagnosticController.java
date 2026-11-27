package com.rubengarcia.industrialfaultcopilot.api;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticService;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticRequest;
import com.rubengarcia.industrialfaultcopilot.diagnostic.dto.DiagnosticResponse;

import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticController {

    private final ObjectProvider<DiagnosticService> diagnosticServiceProvider;

    public DiagnosticController(ObjectProvider<DiagnosticService> diagnosticServiceProvider) {
        this.diagnosticServiceProvider = diagnosticServiceProvider;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DiagnosticResponse> diagnose(
            @Valid @RequestBody DiagnosticRequest request) {

        DiagnosticService diagnosticService = diagnosticServiceProvider.getIfAvailable();

        if (diagnosticService == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Diagnostic service is not configured yet"
            ));
        }

        return diagnosticService.diagnose(request);
    }
}
