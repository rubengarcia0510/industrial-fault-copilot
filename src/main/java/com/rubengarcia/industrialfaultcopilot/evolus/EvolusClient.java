package com.rubengarcia.industrialfaultcopilot.evolus;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class EvolusClient {

    private final WebClient webClient;

    public EvolusClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> get(String path) {
        return webClient
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class);
    }
}
