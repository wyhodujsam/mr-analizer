package com.mranalizer.domain.exception;

/**
 * Signals that the VCS provider's rate limit has been exceeded.
 * Lives in domain so the application/adapter layers can handle it uniformly.
 */
public class ProviderRateLimitException extends ProviderException {

    public ProviderRateLimitException(String message) {
        super(message);
    }

    public ProviderRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
