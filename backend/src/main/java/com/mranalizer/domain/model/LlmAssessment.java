package com.mranalizer.domain.model;

/**
 * Assessment returned by an LLM provider for a merge request.
 * scoreAdjustment: positive or negative delta applied to the base score.
 */
public record LlmAssessment(
        double scoreAdjustment,
        String comment,
        String provider
) {}
