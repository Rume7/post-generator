package com.codehacks.postgen.dto;

import com.codehacks.postgen.model.EssayStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssayFullUpdateRequest {

    @NotBlank(message = "Topic cannot be empty")
    @Size(min = 3, max = 500, message = "Topic must be between 3 and 500 characters")
    private String topic;

    @NotBlank(message = "Content cannot be empty")
    @Size(min = 50, max = 10000, message = "Content must be between 50 and 10000 characters")
    private String content;

    @NotNull(message = "Status cannot be null")
    private EssayStatus status;
}
