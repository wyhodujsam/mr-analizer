package com.mranalizer.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_analyses")
public class ProjectAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectSlug;
    private LocalDateTime analyzedAt;
    private int totalPrs;
    private int automatableCount;
    private int maybeCount;
    private int notSuitableCount;
    private double avgScore;
    private int bddCount;
    private double bddPercent;
    private int sddCount;
    private double sddPercent;

    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    @Column(columnDefinition = "TEXT")
    private String rowsJson;

    public ProjectAnalysisEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectSlug() { return projectSlug; }
    public void setProjectSlug(String projectSlug) { this.projectSlug = projectSlug; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public int getTotalPrs() { return totalPrs; }
    public void setTotalPrs(int totalPrs) { this.totalPrs = totalPrs; }

    public int getAutomatableCount() { return automatableCount; }
    public void setAutomatableCount(int automatableCount) { this.automatableCount = automatableCount; }

    public int getMaybeCount() { return maybeCount; }
    public void setMaybeCount(int maybeCount) { this.maybeCount = maybeCount; }

    public int getNotSuitableCount() { return notSuitableCount; }
    public void setNotSuitableCount(int notSuitableCount) { this.notSuitableCount = notSuitableCount; }

    public double getAvgScore() { return avgScore; }
    public void setAvgScore(double avgScore) { this.avgScore = avgScore; }

    public int getBddCount() { return bddCount; }
    public void setBddCount(int bddCount) { this.bddCount = bddCount; }

    public double getBddPercent() { return bddPercent; }
    public void setBddPercent(double bddPercent) { this.bddPercent = bddPercent; }

    public int getSddCount() { return sddCount; }
    public void setSddCount(int sddCount) { this.sddCount = sddCount; }

    public double getSddPercent() { return sddPercent; }
    public void setSddPercent(double sddPercent) { this.sddPercent = sddPercent; }

    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }

    public String getRowsJson() { return rowsJson; }
    public void setRowsJson(String rowsJson) { this.rowsJson = rowsJson; }
}
