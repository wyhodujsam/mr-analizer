package com.mranalizer.application.dto;

public record AnalysisSummaryDto(
        Long reportId,
        String projectSlug,
        int totalMrs,
        VerdictCount automatable,
        VerdictCount maybe,
        VerdictCount notSuitable
) {
    public record VerdictCount(int count, double percentage) {}
}
