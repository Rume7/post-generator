package com.codehacks.postgen.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // This bean can be used for custom rate limiting scenarios not covered by application.yml
    // Or to define a default RateLimiterConfig
    @Bean
    public RateLimiter essayGenerationRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(3) // 3 requests
                .limitRefreshPeriod(Duration.ofSeconds(5)) // every 5 seconds
                .timeoutDuration(Duration.ofSeconds(1)) // wait up to 1 second
                .build();
        return RateLimiter.of("customEssayRateLimiter", config);
    }
}