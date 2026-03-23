package com.mranalizer.adapter.in.rest.dto;

import com.mranalizer.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;

import com.mranalizer.domain.model.RuleResult;

public record MrDetailResponse(
        Long resultId,
        String externalId,
        String title,
        String description,
        String author,
        String sourceBranch,
        String targetBranch,
        String state,
        LocalDateTime createdAt,
        LocalDateTime mergedAt,
        List<String> labels,
        int additions,
        int deletions,
        int changedFilesCount,
        boolean hasTests,
        double score,
        String verdict,
        List<String> reasons,
        List<String> matchedRules,
        String llmComment,
        String url,
        List<ScoreBreakdownEntry> scoreBreakdown,
        int overallAutomatability,
        List<AnalysisCategory> categories,
        List<HumanOversightItem> humanOversightRequired,
        List<String> whyLlmFriendly,
        List<SummaryAspect> summaryTable,
        boolean hasDetailedAnalysis,
        AnalysisResponse.LlmCostDto llmCost
) {

    public record ScoreBreakdownEntry(
            String rule,
            String type,
            double weight,
            String reason
    ) {}

    public static MrDetailResponse from(AnalysisResult result) {
        MergeRequest mr = result.getMergeRequest();

        // Build score breakdown from ruleResults (with weight), fallback to matchedRules
        List<ScoreBreakdownEntry> breakdown = List.of();
        List<RuleResult> ruleResults = result.getRuleResults();
        if (ruleResults != null && !ruleResults.isEmpty()) {
            breakdown = ruleResults.stream()
                    .map(rr -> new ScoreBreakdownEntry(
                            rr.ruleName(),
                            inferType(rr.ruleName()),
                            rr.weight(),
                            rr.reason()
                    ))
                    .toList();
        } else if (result.getMatchedRules() != null) {
            // Fallback for old results without ruleResults
            breakdown = result.getMatchedRules().stream()
                    .map(rule -> new ScoreBreakdownEntry(
                            rule,
                            inferType(rule),
                            0.0,
                            findReasonForRule(rule, result.getReasons())
                    ))
                    .toList();
        }

        return new MrDetailResponse(
                result.getId(),
                mr.getExternalId(),
                mr.getTitle(),
                mr.getDescription(),
                mr.getAuthor(),
                mr.getSourceBranch(),
                mr.getTargetBranch(),
                mr.getState(),
                mr.getCreatedAt(),
                mr.getMergedAt(),
                mr.getLabels(),
                mr.getDiffStats() != null ? mr.getDiffStats().additions() : 0,
                mr.getDiffStats() != null ? mr.getDiffStats().deletions() : 0,
                mr.getDiffStats() != null ? mr.getDiffStats().changedFilesCount() : 0,
                mr.hasTests(),
                result.getScore(),
                result.getVerdict().name(),
                result.getReasons(),
                result.getMatchedRules(),
                result.getLlmComment(),
                mr.getUrl(),
                breakdown,
                result.getOverallAutomatability(),
                result.getCategories(),
                result.getHumanOversightRequired(),
                result.getWhyLlmFriendly(),
                result.getSummaryTable(),
                result.hasDetailedAnalysis(),
                result.getLlmCost() != null && result.getLlmCost().totalTokens() > 0
                        ? new AnalysisResponse.LlmCostDto(
                                result.getLlmCost().inputTokens(), result.getLlmCost().outputTokens(),
                                result.getLlmCost().cacheReadTokens(), result.getLlmCost().cacheCreationTokens(),
                                result.getLlmCost().costUsd())
                        : null
        );
    }

    private static String inferType(String rule) {
        if (rule == null) return "unknown";
        String lower = rule.toLowerCase();
        if (lower.contains("boost") || lower.contains("has-test") || lower.contains("description-keyword")) {
            return "boost";
        }
        if (lower.contains("penalize") || lower.contains("large-diff") || lower.contains("no-description")) {
            return "penalize";
        }
        if (lower.contains("exclude")) {
            return "exclude";
        }
        return "rule";
    }

    private static String findReasonForRule(String rule, List<String> reasons) {
        if (reasons == null || rule == null) return "";
        String lowerRule = rule.toLowerCase();
        return reasons.stream()
                .filter(r -> r != null && r.toLowerCase().contains(lowerRule))
                .findFirst()
                .orElse("");
    }
}
