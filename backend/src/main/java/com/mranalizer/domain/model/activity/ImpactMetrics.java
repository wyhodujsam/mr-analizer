package com.mranalizer.domain.model.activity;

public record ImpactMetrics(
        int totalAdditions,
        int totalDeletions,
        int totalLines,
        double avgLinesPerPr,
        double addDeleteRatio
) {}
