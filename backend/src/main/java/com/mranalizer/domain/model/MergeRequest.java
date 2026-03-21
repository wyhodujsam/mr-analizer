package com.mranalizer.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core domain object representing a merge/pull request fetched from a VCS provider.
 */
public class MergeRequest {

    private final Long id;
    private final String externalId;
    private final String title;
    private final String description;
    private final String author;
    private final String sourceBranch;
    private final String targetBranch;
    private final String state;
    private final LocalDateTime createdAt;
    private final LocalDateTime mergedAt;
    private final List<String> labels;
    private final List<ChangedFile> changedFiles;
    private final DiffStats diffStats;
    private final boolean hasTests;
    private final boolean ciPassed;
    private final int approvalsCount;
    private final int commentsCount;
    private final String provider;
    private final String url;
    private final String projectSlug;

    private MergeRequest(Builder builder) {
        this.id = builder.id;
        this.externalId = builder.externalId;
        this.title = builder.title;
        this.description = builder.description;
        this.author = builder.author;
        this.sourceBranch = builder.sourceBranch;
        this.targetBranch = builder.targetBranch;
        this.state = builder.state;
        this.createdAt = builder.createdAt;
        this.mergedAt = builder.mergedAt;
        this.labels = builder.labels != null ? List.copyOf(builder.labels) : List.of();
        this.changedFiles = builder.changedFiles != null ? List.copyOf(builder.changedFiles) : List.of();
        this.diffStats = builder.diffStats != null ? builder.diffStats : new DiffStats(0, 0, 0);
        this.hasTests = builder.hasTests;
        this.ciPassed = builder.ciPassed;
        this.approvalsCount = builder.approvalsCount;
        this.commentsCount = builder.commentsCount;
        this.provider = builder.provider;
        this.url = builder.url;
        this.projectSlug = builder.projectSlug;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getSourceBranch() { return sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public String getState() { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getMergedAt() { return mergedAt; }
    public List<String> getLabels() { return labels; }
    public List<ChangedFile> getChangedFiles() { return changedFiles; }
    public DiffStats getDiffStats() { return diffStats; }
    public boolean hasTests() { return hasTests; }
    public boolean isCiPassed() { return ciPassed; }
    public int getApprovalsCount() { return approvalsCount; }
    public int getCommentsCount() { return commentsCount; }
    public String getProvider() { return provider; }
    public String getUrl() { return url; }
    public String getProjectSlug() { return projectSlug; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String externalId;
        private String title;
        private String description;
        private String author;
        private String sourceBranch;
        private String targetBranch;
        private String state;
        private LocalDateTime createdAt;
        private LocalDateTime mergedAt;
        private List<String> labels;
        private List<ChangedFile> changedFiles;
        private DiffStats diffStats;
        private boolean hasTests;
        private boolean ciPassed;
        private int approvalsCount;
        private int commentsCount;
        private String provider;
        private String url;
        private String projectSlug;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder sourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; return this; }
        public Builder targetBranch(String targetBranch) { this.targetBranch = targetBranch; return this; }
        public Builder state(String state) { this.state = state; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder mergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; return this; }
        public Builder labels(List<String> labels) { this.labels = labels; return this; }
        public Builder changedFiles(List<ChangedFile> changedFiles) { this.changedFiles = changedFiles; return this; }
        public Builder diffStats(DiffStats diffStats) { this.diffStats = diffStats; return this; }
        public Builder hasTests(boolean hasTests) { this.hasTests = hasTests; return this; }
        public Builder ciPassed(boolean ciPassed) { this.ciPassed = ciPassed; return this; }
        public Builder approvalsCount(int approvalsCount) { this.approvalsCount = approvalsCount; return this; }
        public Builder commentsCount(int commentsCount) { this.commentsCount = commentsCount; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder projectSlug(String projectSlug) { this.projectSlug = projectSlug; return this; }

        public MergeRequest build() {
            return new MergeRequest(this);
        }
    }
}
