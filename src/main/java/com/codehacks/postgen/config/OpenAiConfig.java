package com.codehacks.postgen.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(AppConfig appConfig) {
        return OpenAiChatModel.builder()
                .apiKey(appConfig.getOpenaiApiKey())
                .modelName(appConfig.getOpenaiModel())
                .build();
    }
} 