package com.codehacks.postgen.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    @Bean
    public AnthropicChatModel anthropicChatModel() {
        return AnthropicChatModel.builder()
                .apiKey("")
                .modelName("")
                .build();
    }
} 