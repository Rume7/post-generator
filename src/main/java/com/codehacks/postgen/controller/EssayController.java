package com.codehacks.postgen.controller;

import com.codehacks.postgen.dto.EssayRequest;
import com.codehacks.postgen.dto.EssayResponse;
import com.codehacks.postgen.service.EssayGenerationService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
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
    private final RateLimiterRegistry rateLimiterRegistry;

    @PostMapping
    public ResponseEntity<?> generateEssay(@Valid @RequestBody EssayRequest request) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("essay-generation");
        
        try {
            String essay = rateLimiter.executeSupplier(() -> 
                essayGenerationService.generateEssay(request.getTopic(), request.getAdditionalContext())
            );
            
            EssayResponse response = EssayResponse.builder()
                    .topic(request.getTopic())
                    .content(essay)
                    .generatedAt(LocalDateTime.now())
                    .modelUsed("gpt-4") // This should come from the service
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }
    }
}