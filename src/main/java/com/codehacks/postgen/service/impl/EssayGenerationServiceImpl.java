package com.codehacks.postgen.service.impl;

import com.codehacks.postgen.service.EssayGenerationService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EssayGenerationServiceImpl implements EssayGenerationService {

    private final OpenAiChatModel openAiModel;
    private final AnthropicChatModel anthropicModel;

    @Override
    public String generateEssay(String topic, String additionalContext) {
        String prompt = "Write an essay on the topic: " + topic;
        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            prompt += "\nAdditional context: " + additionalContext;
        }
        
        // For now, we'll use OpenAI. In the future, we can add logic to choose between models
        return openAiModel.generate(prompt);
    }
} 