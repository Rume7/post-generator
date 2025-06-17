package com.codehacks.postgen.service.impl;

import com.codehacks.postgen.service.EssayGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EssayGenerationServiceImpl implements EssayGenerationService {

    //private final ClaudeService claudeService;

    @Override
    public String generateEssay(String topic, String additionalContext) {
        String prompt = "Write an essay on the topic: " + topic;
        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            prompt += "\nAdditional context: " + additionalContext;
        }
        return null;
        
        //return claudeService.generateResponse(prompt);
    }
} 