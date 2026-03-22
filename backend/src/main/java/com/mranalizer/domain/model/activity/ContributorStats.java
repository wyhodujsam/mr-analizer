package com.mranalizer.domain.model.activity;

import java.util.Map;

public record ContributorStats(
        int totalPrs,
        double avgSize,
        double avgReviewTimeMinutes,
        double weekendPercentage,
        Map<Severity, Long> flagCountBySeverity
) {
}
