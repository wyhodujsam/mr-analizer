package com.mranalizer.domain.exception;

/**
 * Signals an authentication/authorization failure with the VCS provider.
 */
public class ProviderAuthException extends ProviderException {

    public ProviderAuthException(String message) {
        super(message);
    }

    public ProviderAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
