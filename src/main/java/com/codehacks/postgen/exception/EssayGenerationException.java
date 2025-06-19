package com.codehacks.postgen.exception;

/**
 * Exception for essay generation errors.
 */
public class EssayGenerationException extends RuntimeException {

    /**
     * Constructor with message.
     * @param message the error message
     */
    public EssayGenerationException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause.
     * @param message the error message
     * @param cause the cause
     */
    public EssayGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
} 