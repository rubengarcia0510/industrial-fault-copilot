package com.rubengarcia.industrialfaultcopilot.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiConfigurationTests {

    private final GeminiConfiguration configuration = new GeminiConfiguration();

    @Test
    void rejectsMissingApiKeyWithoutBuildingChatClient() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> configuration.geminiDiagnosticProvider(builder, " ")
        );

        assertEquals(
                "Gemini API key is not configured. Set GEMINI_API_KEY.",
                exception.getMessage()
        );
    }

    @Test
    void createsDiagnosticProviderWhenApiKeyIsConfigured() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);

        var provider = configuration.geminiDiagnosticProvider(
                builder,
                "test-api-key"
        );

        assertNotNull(provider);
        verify(builder).build();
    }
}
