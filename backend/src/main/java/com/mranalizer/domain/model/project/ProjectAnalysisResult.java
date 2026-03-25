package com.mranalizer.domain.model.project;

import java.time.LocalDateTime;
import java.util.List;

public class ProjectAnalysisResult {

    private Long id;
    private final String projectSlug;
    private final LocalDateTime analyzedAt;
    private final List<PrAnalysisRow> rows;
    private final ProjectSummary summary;

    public ProjectAnalysisResult(String projectSlug, LocalDateTime analyzedAt,
                                  List<PrAnalysisRow> rows, ProjectSummary summary) {
        this(null, projectSlug, analyzedAt, rows, summary);
    }

    public ProjectAnalysisResult(Long id, String projectSlug, LocalDateTime analyzedAt,
                                  List<PrAnalysisRow> rows, ProjectSummary summary) {
        this.id = id;
        this.projectSlug = projectSlug;
        this.analyzedAt = analyzedAt;
        this.rows = List.copyOf(rows);
        this.summary = summary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectSlug() { return projectSlug; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public List<PrAnalysisRow> getRows() { return rows; }
    public ProjectSummary getSummary() { return summary; }
}
