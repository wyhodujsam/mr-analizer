package com.mranalizer.domain.exception;

/**
 * Thrown when input validation fails at the domain/application boundary.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
