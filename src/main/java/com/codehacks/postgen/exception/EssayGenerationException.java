package com.codehacks.postgen.exception;

public class EssayGenerationException extends RuntimeException {

    public EssayGenerationException(String message) {
        super(message);
    }

    public EssayGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
} 