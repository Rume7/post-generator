package com.codehacks.postgen.controller;

import com.codehacks.postgen.dto.EssayRequest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for essay generation endpoints.
 */
@RestController
@RequestMapping("/api/v1/generate")
@RequiredArgsConstructor
public class EssayController {

    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Endpoint to generate an essay.
     * @param request the essay request
     * @return the response entity
     */
    @PostMapping
    public ResponseEntity<?> generateEssay(@Valid @RequestBody EssayRequest request) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("essay-generation");
        
        return null;
    }
}