package com.mranalizer.domain.model.activity;

import java.time.LocalDate;
import java.util.List;

public record VelocityMetrics(
        double prsPerWeek,
        List<WeeklyCount> weeklyBreakdown,
        String trend
) {
    public record WeeklyCount(LocalDate weekStart, int count) {}
}
