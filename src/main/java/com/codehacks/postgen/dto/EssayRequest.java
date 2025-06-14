package com.codehacks.postgen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EssayRequest {
    
    @NotBlank(message = "Topic is required")
    @Size(min = 3, max = 200, message = "Topic must be between 3 and 200 characters")
    private String topic;
    
    @Size(max = 1000, message = "Additional context must not exceed 1000 characters")
    private String additionalContext;
} 