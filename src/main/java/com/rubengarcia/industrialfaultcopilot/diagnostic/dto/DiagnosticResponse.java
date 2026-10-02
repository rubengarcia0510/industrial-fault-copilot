package com.rubengarcia.industrialfaultcopilot.diagnostic.dto;

import java.util.List;

public record DiagnosticResponse(
        String diagnosis,
        List<String> recommendations,
        List<String> sources,
        boolean escalationRequired
) {
}
