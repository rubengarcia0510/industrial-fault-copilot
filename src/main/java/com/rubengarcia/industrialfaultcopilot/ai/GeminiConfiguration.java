package com.rubengarcia.industrialfaultcopilot.ai;

import com.rubengarcia.industrialfaultcopilot.diagnostic.DiagnosticProvider;
import org.springframework.ai.chat.client.ChatClient;
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
    DiagnosticProvider geminiDiagnosticProvider(ChatClient.Builder chatClientBuilder) {
        return new GeminiDiagnosticAdapter(chatClientBuilder.build());
    }
}
