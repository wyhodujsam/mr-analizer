package com.mranalizer.domain.model;

/**
 * Outcome of evaluating a single scoring rule against a merge request.
 */
public record RuleResult(
        String ruleName,
        boolean matched,
        double weight,
        String reason
) {}
