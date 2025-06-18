package com.codehacks.postgen.service.impl;

import com.codehacks.postgen.service.EssayGenerationService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EssayGenerationServiceImpl implements EssayGenerationService {

    private final AnthropicChatModel anthropicModel;

    @Override
    public String generateEssay(String topic, String additionalContext) {
        String prompt = "Write an essay on the topic: " + topic;
        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            prompt += "\nAdditional context: " + additionalContext;
        }

        return null;
        // return openAiModel.generate(prompt);
    }
} 