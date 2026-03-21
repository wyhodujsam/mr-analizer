package com.mranalizer.adapter.in.rest;

import com.mranalizer.adapter.in.rest.dto.ErrorResponse;
import com.mranalizer.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleInvalidRequest_returns400() {
        InvalidRequestException ex = new InvalidRequestException("bad input");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("bad input", response.getBody().message());
    }

    @Test
    void handleUnreadableMessage_returns400() {
        RuntimeException cause = new RuntimeException("JSON parse error");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("JSON parse error", response.getBody().message());
    }

    @Test
    void handleUnreadableMessage_nullCauseMessage_returnsFallback() {
        RuntimeException cause = new RuntimeException((String) null);
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().message());
    }

    @Test
    void handleBadRequest_illegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("invalid arg");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("invalid arg", response.getBody().message());
    }

    @Test
    void handleRateLimit_returns429() {
        ProviderRateLimitException ex = new ProviderRateLimitException("rate limit exceeded");

        ResponseEntity<ErrorResponse> response = handler.handleRateLimit(ex);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Rate Limit Exceeded", response.getBody().error());
        assertEquals("rate limit exceeded", response.getBody().message());
    }

    @Test
    void handleProviderAuth_returns401() {
        ProviderAuthException ex = new ProviderAuthException("bad token");

        ResponseEntity<ErrorResponse> response = handler.handleProviderAuth(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("bad token", response.getBody().message());
    }

    @Test
    void handleProviderError_returns502() {
        ProviderException ex = new ProviderException("GitHub is down");

        ResponseEntity<ErrorResponse> response = handler.handleProviderError(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Provider Error", response.getBody().error());
        assertEquals("GitHub is down", response.getBody().message());
    }

    @Test
    void handleNotFound_returns404() {
        ReportNotFoundException ex = new ReportNotFoundException("Report not found: 42");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Report not found: 42", response.getBody().message());
    }

    @Test
    void handleGeneric_returns500() {
        Exception ex = new Exception("unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
