package com.rubengarcia.industrialfaultcopilot.ai;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "spring.ai.model",
        name = "chat",
        havingValue = "google-genai"
)
public class GeminiConfiguration {

    @Bean
    DiagnosticProvider geminiDiagnosticProvider(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.google.genai.api-key:}") String apiKey
    ) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is not configured. Set GEMINI_API_KEY."
            );
        }

        return new GeminiDiagnosticAdapter(chatClientBuilder.build());
    }
}
