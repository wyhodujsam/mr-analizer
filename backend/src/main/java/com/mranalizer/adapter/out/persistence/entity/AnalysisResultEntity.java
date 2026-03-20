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

    public AnalysisReportEntity getReport() { return report; }
    public void setReport(AnalysisReportEntity report) { this.report = report; }
}
