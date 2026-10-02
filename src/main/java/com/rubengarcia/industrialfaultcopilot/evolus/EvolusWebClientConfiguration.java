package com.rubengarcia.industrialfaultcopilot.evolus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EvolusWebClientConfiguration {

    @Bean
    WebClient.Builder evolusWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    EvolusClient evolusClient(
            WebClient.Builder builder,
            EvolusProperties properties) {

        WebClient webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader("X-ApiKey", properties.apiKey())
                .defaultHeader("X-Application-Id", properties.applicationId())
                .build();

        return new EvolusClient(webClient);
    }
}
