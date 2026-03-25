package com.mranalizer.domain.model.activity;

public record ReviewEngagementMetrics(
        int reviewsGiven,
        int reviewsReceived,
        double ratio,
        String label
) {}
