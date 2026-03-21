package com.mranalizer.domain.model;

/**
 * A category of changes in a PR with its automatability score and reasoning.
 */
public record AnalysisCategory(
        String name,
        int score,
        String reasoning
) {}
