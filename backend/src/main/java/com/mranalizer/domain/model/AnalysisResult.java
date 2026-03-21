package com.mranalizer.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of analysing a single merge request — score, verdict, and supporting detail.
 */
public class AnalysisResult {

    private final Long id;
    private final MergeRequest mergeRequest;
    private final double score;
    private final Verdict verdict;
    private final List<String> reasons;
    private final List<String> matchedRules;
    private final String llmComment;
    private final LocalDateTime analyzedAt;
    private final int overallAutomatability;
    private final List<AnalysisCategory> categories;
    private final List<HumanOversightItem> humanOversightRequired;
    private final List<String> whyLlmFriendly;
    private final List<SummaryAspect> summaryTable;

    private AnalysisResult(Builder builder) {
        this.id = builder.id;
        this.mergeRequest = builder.mergeRequest;
        this.score = builder.score;
        this.verdict = builder.verdict;
        this.reasons = builder.reasons;
        this.matchedRules = builder.matchedRules;
        this.llmComment = builder.llmComment;
        this.analyzedAt = builder.analyzedAt;
        this.overallAutomatability = builder.overallAutomatability;
        this.categories = builder.categories != null ? builder.categories : List.of();
        this.humanOversightRequired = builder.humanOversightRequired != null ? builder.humanOversightRequired : List.of();
        this.whyLlmFriendly = builder.whyLlmFriendly != null ? builder.whyLlmFriendly : List.of();
        this.summaryTable = builder.summaryTable != null ? builder.summaryTable : List.of();
    }

    public Long getId() { return id; }
    public MergeRequest getMergeRequest() { return mergeRequest; }
    public double getScore() { return score; }
    public Verdict getVerdict() { return verdict; }
    public List<String> getReasons() { return reasons; }
    public List<String> getMatchedRules() { return matchedRules; }
    public String getLlmComment() { return llmComment; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public int getOverallAutomatability() { return overallAutomatability; }
    public List<AnalysisCategory> getCategories() { return categories; }
    public List<HumanOversightItem> getHumanOversightRequired() { return humanOversightRequired; }
    public List<String> getWhyLlmFriendly() { return whyLlmFriendly; }
    public List<SummaryAspect> getSummaryTable() { return summaryTable; }

    public boolean hasDetailedAnalysis() {
        return overallAutomatability > 0 || !categories.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private MergeRequest mergeRequest;
        private double score;
        private Verdict verdict;
        private List<String> reasons;
        private List<String> matchedRules;
        private String llmComment;
        private LocalDateTime analyzedAt;
        private int overallAutomatability;
        private List<AnalysisCategory> categories;
        private List<HumanOversightItem> humanOversightRequired;
        private List<String> whyLlmFriendly;
        private List<SummaryAspect> summaryTable;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder mergeRequest(MergeRequest mergeRequest) { this.mergeRequest = mergeRequest; return this; }
        public Builder score(double score) { this.score = score; return this; }
        public Builder verdict(Verdict verdict) { this.verdict = verdict; return this; }
        public Builder reasons(List<String> reasons) { this.reasons = reasons; return this; }
        public Builder matchedRules(List<String> matchedRules) { this.matchedRules = matchedRules; return this; }
        public Builder llmComment(String llmComment) { this.llmComment = llmComment; return this; }
        public Builder analyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; return this; }
        public Builder overallAutomatability(int overallAutomatability) { this.overallAutomatability = overallAutomatability; return this; }
        public Builder categories(List<AnalysisCategory> categories) { this.categories = categories; return this; }
        public Builder humanOversightRequired(List<HumanOversightItem> humanOversightRequired) { this.humanOversightRequired = humanOversightRequired; return this; }
        public Builder whyLlmFriendly(List<String> whyLlmFriendly) { this.whyLlmFriendly = whyLlmFriendly; return this; }
        public Builder summaryTable(List<SummaryAspect> summaryTable) { this.summaryTable = summaryTable; return this; }

        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }
}
