package com.mranalizer.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated report for a batch analysis run over a set of merge requests.
 * Counts (automatableCount, maybeCount, notSuitableCount) are computed from
 * the provided results list when using the factory method {@link #of}.
 */
public class AnalysisReport {

    private final Long id;
    private final String projectSlug;
    private final String provider;
    private final LocalDateTime analyzedAt;
    private final int totalMrs;
    private final List<AnalysisResult> results;
    private final int automatableCount;
    private final int maybeCount;
    private final int notSuitableCount;

    private AnalysisReport(Builder builder) {
        this.id = builder.id;
        this.projectSlug = builder.projectSlug;
        this.provider = builder.provider;
        this.analyzedAt = builder.analyzedAt;
        this.totalMrs = builder.totalMrs;
        this.results = builder.results;
        this.automatableCount = builder.automatableCount;
        this.maybeCount = builder.maybeCount;
        this.notSuitableCount = builder.notSuitableCount;
    }

    /**
     * Factory method that creates a report and computes verdict counts from the results list.
     */
    public static AnalysisReport of(Long id,
                                    String projectSlug,
                                    String provider,
                                    LocalDateTime analyzedAt,
                                    List<AnalysisResult> results) {
        int automatable = 0;
        int maybe = 0;
        int notSuitable = 0;

        if (results != null) {
            for (AnalysisResult r : results) {
                if (r.getVerdict() == Verdict.AUTOMATABLE) automatable++;
                else if (r.getVerdict() == Verdict.MAYBE) maybe++;
                else if (r.getVerdict() == Verdict.NOT_SUITABLE) notSuitable++;
            }
        }

        return new Builder()
                .id(id)
                .projectSlug(projectSlug)
                .provider(provider)
                .analyzedAt(analyzedAt)
                .totalMrs(results == null ? 0 : results.size())
                .results(results)
                .automatableCount(automatable)
                .maybeCount(maybe)
                .notSuitableCount(notSuitable)
                .build();
    }

    public Long getId() { return id; }
    public String getProjectSlug() { return projectSlug; }
    public String getProvider() { return provider; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public int getTotalMrs() { return totalMrs; }
    public List<AnalysisResult> getResults() { return results; }
    public int getAutomatableCount() { return automatableCount; }
    public int getMaybeCount() { return maybeCount; }
    public int getNotSuitableCount() { return notSuitableCount; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String projectSlug;
        private String provider;
        private LocalDateTime analyzedAt;
        private int totalMrs;
        private List<AnalysisResult> results;
        private int automatableCount;
        private int maybeCount;
        private int notSuitableCount;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder projectSlug(String projectSlug) { this.projectSlug = projectSlug; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder analyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; return this; }
        public Builder totalMrs(int totalMrs) { this.totalMrs = totalMrs; return this; }
        public Builder results(List<AnalysisResult> results) { this.results = results; return this; }
        public Builder automatableCount(int automatableCount) { this.automatableCount = automatableCount; return this; }
        public Builder maybeCount(int maybeCount) { this.maybeCount = maybeCount; return this; }
        public Builder notSuitableCount(int notSuitableCount) { this.notSuitableCount = notSuitableCount; return this; }

        public AnalysisReport build() {
            return new AnalysisReport(this);
        }
    }
}
