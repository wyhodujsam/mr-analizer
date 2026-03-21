package com.mranalizer.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void providerException_messageConstructor() {
        ProviderException ex = new ProviderException("provider error");
        assertEquals("provider error", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void providerException_messageAndCauseConstructor() {
        Throwable cause = new RuntimeException("root cause");
        ProviderException ex = new ProviderException("provider error", cause);
        assertEquals("provider error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void providerRateLimitException_extendsProviderException() {
        ProviderRateLimitException ex = new ProviderRateLimitException("rate limited");
        assertEquals("rate limited", ex.getMessage());
        assertInstanceOf(ProviderException.class, ex);
    }

    @Test
    void providerRateLimitException_messageAndCauseConstructor() {
        Throwable cause = new RuntimeException("cause");
        ProviderRateLimitException ex = new ProviderRateLimitException("rate limited", cause);
        assertEquals("rate limited", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void providerAuthException_extendsProviderException() {
        ProviderAuthException ex = new ProviderAuthException("auth failed");
        assertEquals("auth failed", ex.getMessage());
        assertInstanceOf(ProviderException.class, ex);
    }

    @Test
    void providerAuthException_messageAndCauseConstructor() {
        Throwable cause = new RuntimeException("cause");
        ProviderAuthException ex = new ProviderAuthException("auth failed", cause);
        assertEquals("auth failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void reportNotFoundException_extendsRuntimeException() {
        ReportNotFoundException ex = new ReportNotFoundException("not found");
        assertEquals("not found", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
        assertFalse(ProviderException.class.isAssignableFrom(ReportNotFoundException.class));
    }

    @Test
    void invalidRequestException_extendsRuntimeException() {
        InvalidRequestException ex = new InvalidRequestException("bad request");
        assertEquals("bad request", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
        assertFalse(ProviderException.class.isAssignableFrom(InvalidRequestException.class));
    }
}
