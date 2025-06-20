package com.codehacks.postgen;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.chat.client.ChatClient;
import org.mockito.Mockito;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public ChatClient mockChatClient() {
        ChatClient mockClient = Mockito.mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

        Mockito.when(mockClient.prompt(Mockito.anyString())).thenReturn(requestSpec);
        Mockito.when(requestSpec.call()).thenReturn(callResponseSpec);
        Mockito.when(callResponseSpec.content()).thenReturn(
            "This is a test essay generated for integration testing purposes. " +
            "It contains multiple sentences to meet the minimum word count requirement. " +
            "The content is structured and coherent, providing a realistic test scenario " +
            "for the essay generation functionality. This mock response ensures that " +
            "integration tests can run without requiring actual AI service credentials."
        );
        return mockClient;
    }

    @Bean
    @Primary
    public ChatClient.Builder mockChatClientBuilder(ChatClient mockChatClient) {
        ChatClient.Builder mockBuilder = Mockito.mock(ChatClient.Builder.class);
        Mockito.when(mockBuilder.build()).thenReturn(mockChatClient);
        return mockBuilder;
    }
} 