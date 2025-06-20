package com.codehacks.postgen.dto;

import com.codehacks.postgen.model.EssayStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EssayUpdateStatusRequest {

    @NotNull(message = "Status cannot be null")
    private EssayStatus status;

}