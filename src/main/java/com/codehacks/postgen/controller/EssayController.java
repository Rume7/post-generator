package com.codehacks.postgen.controller;

import com.codehacks.postgen.dto.EssayRequest;
import com.codehacks.postgen.dto.EssayResponse;
import com.codehacks.postgen.service.EssayGenerationService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/essay")
@RequiredArgsConstructor
public class EssayController {

    private final EssayGenerationService essayGenerationService;
    private final Bucket bucket;

    @PostMapping
    public ResponseEntity<?> generateEssay(@Valid @RequestBody EssayRequest request) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (!probe.isConsumed()) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-Rate-Limit-Retry-After-Seconds", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000))
                    .body("Rate limit exceeded. Try again in " + probe.getNanosToWaitForRefill() / 1_000_000_000 + " seconds");
        }

        String essay = essayGenerationService.generateEssay(request.getTopic(), request.getAdditionalContext());
        
        EssayResponse response = EssayResponse.builder()
                .topic(request.getTopic())
                .content(essay)
                .generatedAt(LocalDateTime.now())
                .modelUsed("gpt-4") // This should come from the service
                .build();

        return ResponseEntity.ok(response);
    }
}