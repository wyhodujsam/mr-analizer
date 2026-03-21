package com.mranalizer.domain.exception;

/**
 * Thrown when a requested analysis report or result does not exist.
 */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String message) {
        super(message);
    }
}
