package com.mranalizer.adapter.in.rest.dto;

public record ErrorResponse(
        String error,
        String message
) {}
