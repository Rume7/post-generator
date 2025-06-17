package com.codehacks.postgen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EssayResponse {

    private String topic;
    private String content;
    private LocalDateTime generatedAt;
    private String modelUsed;

} 