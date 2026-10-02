package com.rubengarcia.industrialfaultcopilot.evolus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evolus")
public record EvolusProperties(
        String baseUrl,
        String apiKey,
        String applicationId,
        String agentId
) {
}
