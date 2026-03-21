package com.mranalizer.domain.exception;

/**
 * Domain-level exception for VCS provider failures (GitHub, GitLab, …).
 * Adapters should wrap provider-specific errors into this exception
 * so the application layer stays decoupled from adapter internals.
 */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
