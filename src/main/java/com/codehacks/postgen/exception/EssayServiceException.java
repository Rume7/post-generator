package com.codehacks.postgen.exception;

/**
 * Exception for essay service errors.
 */
public class EssayServiceException extends RuntimeException {
    /**
     * Constructor with message.
     * @param message the error message
     */
    public EssayServiceException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause.
     * @param message the error message
     * @param cause the cause
     */
    public EssayServiceException(String message, Throwable cause) {
        super(message, cause);
    }
} 