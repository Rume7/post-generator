package com.codehacks.postgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    
    @NotBlank(message = "Database URL is required")
    private String dbUrl;
    
    @NotBlank(message = "Database username is required")
    private String dbUsername;
    
    @NotBlank(message = "Database password is required")
    private String dbPassword;
    
    @NotBlank(message = "OpenAI API key is required")
    private String openaiApiKey;
    
    @NotBlank(message = "OpenAI model is required")
    private String openaiModel;
    
    @NotBlank(message = "Anthropic API key is required")
    private String anthropicApiKey;
    
    @NotBlank(message = "Anthropic model is required")
    private String anthropicModel;
} 