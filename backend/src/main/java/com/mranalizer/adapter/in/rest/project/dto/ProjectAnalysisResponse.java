package com.mranalizer.adapter.in.rest.project.dto;

import com.mranalizer.domain.model.project.*;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectAnalysisResponse(
        Long id,
        String projectSlug,
        LocalDateTime analyzedAt,
        SummaryResponse summary,
        List<PrRowResponse> rows
) {

    public static ProjectAnalysisResponse from(ProjectAnalysisResult result) {
        ProjectSummary s = result.getSummary();
        SummaryResponse summary = new SummaryResponse(
                s.totalPrs(),
                s.automatableCount(), s.maybeCount(), s.notSuitableCount(),
                s.automatablePercent(), s.maybePercent(), s.notSuitablePercent(),
                s.avgScore(),
                s.topRules().stream()
                        .map(r -> new RuleFrequencyResponse(r.ruleName(), r.matchCount(), r.avgWeight()))
                        .toList(),
                s.histogram().stream()
                        .map(b -> new HistogramBucketResponse(b.rangeStart(), b.rangeEnd(), b.count()))
                        .toList(),
                s.bddCount(), s.bddPercent(),
                s.sddCount(), s.sddPercent()
        );

        List<PrRowResponse> rows = result.getRows().stream()
                .map(r -> new PrRowResponse(
                        r.prId(), r.title(), r.author(), r.state(), r.url(),
                        r.createdAt(), r.mergedAt(), r.additions(), r.deletions(),
                        r.aiScore(), r.aiVerdict().name(),
                        r.ruleResults().stream()
                                .map(rr -> new RuleResultResponse(rr.ruleName(), rr.matched(), rr.weight(), rr.reason()))
                                .toList(),
                        r.llmComment(),
                        r.hasBdd(), r.hasSdd(), r.bddFiles(), r.sddFiles()))
                .toList();

        return new ProjectAnalysisResponse(result.getId(), result.getProjectSlug(), result.getAnalyzedAt(), summary, rows);
    }

    public record SummaryResponse(
            int totalPrs,
            int automatableCount, int maybeCount, int notSuitableCount,
            double automatablePercent, double maybePercent, double notSuitablePercent,
            double avgScore,
            List<RuleFrequencyResponse> topRules,
            List<HistogramBucketResponse> histogram,
            int bddCount, double bddPercent,
            int sddCount, double sddPercent
    ) {}

    public record RuleFrequencyResponse(String ruleName, int matchCount, double avgWeight) {}
    public record HistogramBucketResponse(double rangeStart, double rangeEnd, int count) {}
    public record RuleResultResponse(String ruleName, boolean matched, double weight, String reason) {}

    public record PrRowResponse(
            String prId, String title, String author, String state, String url,
            LocalDateTime createdAt, LocalDateTime mergedAt,
            int additions, int deletions,
            double aiScore, String aiVerdict,
            List<RuleResultResponse> ruleResults,
            String llmComment,
            boolean hasBdd, boolean hasSdd,
            List<String> bddFiles, List<String> sddFiles
    ) {}
}
