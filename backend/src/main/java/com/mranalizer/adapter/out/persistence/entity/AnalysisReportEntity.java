package com.mranalizer.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analysis_reports")
public class AnalysisReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectSlug;
    private String provider;
    private LocalDateTime analyzedAt;
    private int totalMrs;
    private int automatableCount;
    private int maybeCount;
    private int notSuitableCount;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisResultEntity> results = new ArrayList<>();

    public AnalysisReportEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectSlug() { return projectSlug; }
    public void setProjectSlug(String projectSlug) { this.projectSlug = projectSlug; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public int getTotalMrs() { return totalMrs; }
    public void setTotalMrs(int totalMrs) { this.totalMrs = totalMrs; }

    public int getAutomatableCount() { return automatableCount; }
    public void setAutomatableCount(int automatableCount) { this.automatableCount = automatableCount; }

    public int getMaybeCount() { return maybeCount; }
    public void setMaybeCount(int maybeCount) { this.maybeCount = maybeCount; }

    public int getNotSuitableCount() { return notSuitableCount; }
    public void setNotSuitableCount(int notSuitableCount) { this.notSuitableCount = notSuitableCount; }

    public List<AnalysisResultEntity> getResults() { return results; }
    public void setResults(List<AnalysisResultEntity> results) { this.results = results; }
}
