package com.codehacks.postgen.dto;

import com.codehacks.postgen.model.EssayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for essay response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EssayResponse {

    private Long id;
    private String topic;
    private String content;
    private Integer lengthWords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EssayStatus status;

} 