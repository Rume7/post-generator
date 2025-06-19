package com.codehacks.postgen.exception;

/**
 * Exception for duplicate essay topic errors.
 */
public class DuplicateEssayTopicException extends RuntimeException {

    /**
     * Constructor with message.
     * @param message the error message
     */
    public DuplicateEssayTopicException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause.
     * @param message the error message
     * @param cause the cause
     */
    public DuplicateEssayTopicException(String message, Throwable cause) {
        super(message, cause);
    }
} 