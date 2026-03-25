package com.mranalizer.domain.model.activity;

public record CycleTimeMetrics(
        double avgHours,
        double medianHours,
        double p90Hours
) {}
