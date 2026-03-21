package com.mranalizer.adapter.in.rest.dto;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record AnalysisResponse(
        Long reportId,
        String projectSlug,
        String provider,
        LocalDateTime analyzedAt,
        int totalMrs,
        int automatableCount,
        int maybeCount,
        int notSuitableCount,
        List<ResultItem> results
) {

    public record ResultItem(
            Long id,
            String externalId,
            String title,
            String author,
            double score,
            String verdict,
            List<String> reasons,
            List<String> matchedRules,
            String llmComment,
            String url,
            boolean hasDetailedAnalysis
    ) {
        public static ResultItem from(AnalysisResult r) {
            return new ResultItem(
                    r.getId(),
                    r.getMergeRequest().getExternalId(),
                    r.getMergeRequest().getTitle(),
                    r.getMergeRequest().getAuthor(),
                    r.getScore(),
                    r.getVerdict().name(),
                    r.getReasons(),
                    r.getMatchedRules(),
                    r.getLlmComment(),
                    r.getMergeRequest().getUrl(),
                    r.hasDetailedAnalysis()
            );
        }
    }

    public static AnalysisResponse from(AnalysisReport report) {
        List<ResultItem> items = report.getResults() != null
                ? report.getResults().stream()
                    .map(ResultItem::from)
                    .collect(Collectors.toList())
                : List.of();

        return new AnalysisResponse(
                report.getId(),
                report.getProjectSlug(),
                report.getProvider(),
                report.getAnalyzedAt(),
                report.getTotalMrs(),
                report.getAutomatableCount(),
                report.getMaybeCount(),
                report.getNotSuitableCount(),
                items
        );
    }
}
