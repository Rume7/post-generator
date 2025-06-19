package com.codehacks.postgen.exception;

public class DuplicateEssayTopicException extends RuntimeException {

    public DuplicateEssayTopicException(String message) {
        super(message);
    }

    public DuplicateEssayTopicException(String message, Throwable cause) {
        super(message, cause);
    }
} 