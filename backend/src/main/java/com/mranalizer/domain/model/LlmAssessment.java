package com.mranalizer.domain.model;

import java.util.List;

public record LlmAssessment(
        double scoreAdjustment,
        String comment,
        String provider,
        int overallAutomatability,
        List<AnalysisCategory> categories,
        List<HumanOversightItem> humanOversightRequired,
        List<String> whyLlmFriendly,
        List<SummaryAspect> summaryTable,
        LlmCost cost
) {

    public LlmAssessment(double scoreAdjustment, String comment, String provider) {
        this(scoreAdjustment, comment, provider, 0, List.of(), List.of(), List.of(), List.of(), LlmCost.empty());
    }

    public LlmAssessment(double scoreAdjustment, String comment, String provider,
                          int overallAutomatability, List<AnalysisCategory> categories,
                          List<HumanOversightItem> humanOversightRequired,
                          List<String> whyLlmFriendly, List<SummaryAspect> summaryTable) {
        this(scoreAdjustment, comment, provider, overallAutomatability, categories,
                humanOversightRequired, whyLlmFriendly, summaryTable, LlmCost.empty());
    }
}
