package com.mranalizer.domain.model.activity;

public record ProductivityMetrics(
        VelocityMetrics velocity,
        CycleTimeMetrics cycleTime,
        ImpactMetrics impact,
        CodeChurnMetrics codeChurn,
        ReviewEngagementMetrics reviewEngagement
) {}
