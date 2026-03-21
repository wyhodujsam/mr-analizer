package com.mranalizer.adapter.in.rest.dto;

import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.model.MergeRequest;

import java.time.LocalDateTime;
import java.util.List;

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
        List<ScoreBreakdownEntry> scoreBreakdown
) {

    public record ScoreBreakdownEntry(
            String rule,
            String type,
            double weight,
            String reason
    ) {}

    public static MrDetailResponse from(AnalysisResult result) {
        MergeRequest mr = result.getMergeRequest();

        // Build score breakdown from matched rules and reasons
        List<ScoreBreakdownEntry> breakdown = List.of();
        if (result.getMatchedRules() != null) {
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
                mr.isHasTests(),
                result.getScore(),
                result.getVerdict().name(),
                result.getReasons(),
                result.getMatchedRules(),
                result.getLlmComment(),
                mr.getUrl(),
                breakdown
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
