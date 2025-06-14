package com.codehacks.postgen.service;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class EssayGenerationService {

    private final OpenAiChatModel openAiModel;
    private final AnthropicChatModel anthropicModel;

    public EssayGenerationService() {
        openAiModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        anthropicModel = AnthropicChatModel.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .modelName("claude-3")
                .build();
    }

    public String generateEssay(String topic) {
        // Use either OpenAI or Anthropic model to generate the essay
        return openAiModel.chat("Write an essay on the topic: " + topic);
    }

}
