package com.mranalizer.domain.model.project;

import java.util.List;

public record ProjectSummary(
        int totalPrs,
        int automatableCount,
        int maybeCount,
        int notSuitableCount,
        double automatablePercent,
        double maybePercent,
        double notSuitablePercent,
        double avgScore,
        List<RuleFrequency> topRules,
        List<ScoreHistogramBucket> histogram,
        int bddCount,
        double bddPercent,
        int sddCount,
        double sddPercent
) {}
