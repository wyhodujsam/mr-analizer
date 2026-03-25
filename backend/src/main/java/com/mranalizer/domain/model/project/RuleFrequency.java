package com.mranalizer.domain.model.project;

public record RuleFrequency(
        String ruleName,
        int matchCount,
        double avgWeight
) {}
