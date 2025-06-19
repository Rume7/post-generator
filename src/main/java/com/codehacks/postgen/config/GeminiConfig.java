package com.codehacks.postgen.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
// For free Google AI Studio API:
// import org.springframework.ai.openai.OpenAiChatModel;
// import org.springframework.ai.openai.api.OpenAiApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Google Gemini (Vertex AI) ChatClient.
 */
@Configuration
public class GeminiConfig {

    /**
     * Configures a ChatClient bean for Google Gemini (Vertex AI).
     *
     * To use this:
     * 1. Ensure 'spring-ai-vertex-ai-gemini-spring-boot-starter' is in your pom.xml.
     * 2. Set the following in application.yml:
     * spring.ai.vertex.ai.gemini.project-id: your-google-cloud-project-id
     * spring.ai.vertex.ai.gemini.location: us-central1 (or your region)
     * spring.ai.vertex.ai.gemini.chat.options.model: gemini-pro (or gemini-1.5-pro, etc.)
     * spring.ai.vertex.ai.gemini.api-key: ${GEMINI_API_KEY} (or use Application Default Credentials)
     *
     * Note: For local development with Vertex AI, Google recommends Application Default Credentials (ADC)
     * rather than an API key directly. You'd typically use `gcloud auth application-default login`.
     * An API key might be used for the public `generativelanguage.googleapis.com` endpoint (AI Studio).
     *
     * This bean creates a ChatClient that can be injected into your services.
     */
    @Bean
    public ChatClient geminiChatClient(VertexAiGeminiChatModel chatModel) {
        // You can add more customization to the ChatClient.Builder if needed,
        // such as default system prompts, or tool integration.
        return ChatClient.builder(chatModel).build();
    }


    /*
    // --- ALTERNATIVE: For free Google AI Studio API (OpenAI Compatible Endpoint) ---
    // If you choose this, make sure 'spring-ai-openai-spring-boot-starter' is in your pom.xml instead.
    // And set the following in application.yml:
    // spring.ai.openai.api-key: ${GEMINI_API_KEY}
    // spring.ai.openai.base-url: https://generativelanguage.googleapis.com/v1beta/openai
    // spring.ai.openai.chat.completions-path: /chat/completions
    // spring.ai.openai.chat.options.model: gemini-pro (or gemini-1.5-pro, gemini-1.5-flash, etc.)

    @Bean
    public ChatClient geminiAiStudioChatClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.completions-path}") String completionsPath) {

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi);
        return ChatClient.builder(chatModel).build();
    }
    */
}