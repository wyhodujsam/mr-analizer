package com.mranalizer.domain.model.project;

public record ScoreHistogramBucket(
        double rangeStart,
        double rangeEnd,
        int count
) {}
