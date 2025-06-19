package com.codehacks.postgen.exception;

public class EssayServiceException extends RuntimeException {
    public EssayServiceException(String message) {
        super(message);
    }
    
    public EssayServiceException(String message, Throwable cause) {
        super(message, cause);
    }
} 