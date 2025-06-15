package com.codehacks.postgen.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    @Bean
    public AnthropicChatModel anthropicChatModel(AppConfig appConfig) {
        return AnthropicChatModel.builder()
                .apiKey(appConfig.getAnthropicApiKey())
                .modelName(appConfig.getAnthropicModel())
                .build();
    }
} 