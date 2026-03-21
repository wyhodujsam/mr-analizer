package com.mranalizer.domain.model;

import java.util.List;

/**
 * Assessment returned by an LLM provider for a merge request.
 * scoreAdjustment: positive or negative delta applied to the base score.
 * Extended with detailed analysis fields (categories, oversight, etc.).
 */
public record LlmAssessment(
        double scoreAdjustment,
        String comment,
        String provider,
        int overallAutomatability,
        List<AnalysisCategory> categories,
        List<HumanOversightItem> humanOversightRequired,
        List<String> whyLlmFriendly,
        List<SummaryAspect> summaryTable
) {

    /**
     * Simple constructor for backward compatibility (no detailed analysis).
     */
    public LlmAssessment(double scoreAdjustment, String comment, String provider) {
        this(scoreAdjustment, comment, provider, 0, List.of(), List.of(), List.of(), List.of());
    }
}
