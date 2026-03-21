package com.mranalizer.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalMrId;
    private String mrTitle;
    private String mrAuthor;
    private String projectSlug;
    private String provider;
    private double score;
    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(columnDefinition = "TEXT")
    private String matchedRules;

    @Column(columnDefinition = "TEXT")
    private String llmComment;

    private LocalDateTime analyzedAt;
    private String mrUrl;

    // MR metadata fields
    @Column(columnDefinition = "TEXT")
    private String mrDescription;
    private String mrSourceBranch;
    private String mrTargetBranch;
    private String mrState;
    private LocalDateTime mrCreatedAt;
    private LocalDateTime mrMergedAt;
    @Column(columnDefinition = "TEXT")
    private String mrLabels;
    private int mrAdditions;
    private int mrDeletions;
    private int mrChangedFilesCount;
    private boolean mrHasTests;

    // Detailed LLM analysis fields
    private Integer overallAutomatability;

    @Column(columnDefinition = "TEXT")
    private String categories;

    @Column(columnDefinition = "TEXT")
    private String humanOversightRequired;

    @Column(columnDefinition = "TEXT")
    private String whyLlmFriendly;

    @Column(columnDefinition = "TEXT")
    private String summaryTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private AnalysisReportEntity report;

    public AnalysisResultEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExternalMrId() { return externalMrId; }
    public void setExternalMrId(String externalMrId) { this.externalMrId = externalMrId; }

    public String getMrTitle() { return mrTitle; }
    public void setMrTitle(String mrTitle) { this.mrTitle = mrTitle; }

    public String getMrAuthor() { return mrAuthor; }
    public void setMrAuthor(String mrAuthor) { this.mrAuthor = mrAuthor; }

    public String getProjectSlug() { return projectSlug; }
    public void setProjectSlug(String projectSlug) { this.projectSlug = projectSlug; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }

    public String getMatchedRules() { return matchedRules; }
    public void setMatchedRules(String matchedRules) { this.matchedRules = matchedRules; }

    public String getLlmComment() { return llmComment; }
    public void setLlmComment(String llmComment) { this.llmComment = llmComment; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public String getMrUrl() { return mrUrl; }
    public void setMrUrl(String mrUrl) { this.mrUrl = mrUrl; }

    public String getMrDescription() { return mrDescription; }
    public void setMrDescription(String mrDescription) { this.mrDescription = mrDescription; }

    public String getMrSourceBranch() { return mrSourceBranch; }
    public void setMrSourceBranch(String mrSourceBranch) { this.mrSourceBranch = mrSourceBranch; }

    public String getMrTargetBranch() { return mrTargetBranch; }
    public void setMrTargetBranch(String mrTargetBranch) { this.mrTargetBranch = mrTargetBranch; }

    public String getMrState() { return mrState; }
    public void setMrState(String mrState) { this.mrState = mrState; }

    public LocalDateTime getMrCreatedAt() { return mrCreatedAt; }
    public void setMrCreatedAt(LocalDateTime mrCreatedAt) { this.mrCreatedAt = mrCreatedAt; }

    public LocalDateTime getMrMergedAt() { return mrMergedAt; }
    public void setMrMergedAt(LocalDateTime mrMergedAt) { this.mrMergedAt = mrMergedAt; }

    public String getMrLabels() { return mrLabels; }
    public void setMrLabels(String mrLabels) { this.mrLabels = mrLabels; }

    public int getMrAdditions() { return mrAdditions; }
    public void setMrAdditions(int mrAdditions) { this.mrAdditions = mrAdditions; }

    public int getMrDeletions() { return mrDeletions; }
    public void setMrDeletions(int mrDeletions) { this.mrDeletions = mrDeletions; }

    public int getMrChangedFilesCount() { return mrChangedFilesCount; }
    public void setMrChangedFilesCount(int mrChangedFilesCount) { this.mrChangedFilesCount = mrChangedFilesCount; }

    public boolean isMrHasTests() { return mrHasTests; }
    public void setMrHasTests(boolean mrHasTests) { this.mrHasTests = mrHasTests; }

    public Integer getOverallAutomatability() { return overallAutomatability; }
    public void setOverallAutomatability(Integer overallAutomatability) { this.overallAutomatability = overallAutomatability; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public String getHumanOversightRequired() { return humanOversightRequired; }
    public void setHumanOversightRequired(String humanOversightRequired) { this.humanOversightRequired = humanOversightRequired; }

    public String getWhyLlmFriendly() { return whyLlmFriendly; }
    public void setWhyLlmFriendly(String whyLlmFriendly) { this.whyLlmFriendly = whyLlmFriendly; }

    public String getSummaryTable() { return summaryTable; }
    public void setSummaryTable(String summaryTable) { this.summaryTable = summaryTable; }

    public AnalysisReportEntity getReport() { return report; }
    public void setReport(AnalysisReportEntity report) { this.report = report; }
}
