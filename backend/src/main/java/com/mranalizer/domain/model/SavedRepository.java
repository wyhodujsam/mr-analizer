package com.mranalizer.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a saved (bookmarked) repository for browsing and analysis.
 */
public class SavedRepository {

    private final Long id;
    private final String projectSlug;
    private final String provider;
    private final LocalDateTime addedAt;
    private final LocalDateTime lastAnalyzedAt;

    private SavedRepository(Builder builder) {
        this.id = builder.id;
        this.projectSlug = builder.projectSlug;
        this.provider = builder.provider;
        this.addedAt = builder.addedAt;
        this.lastAnalyzedAt = builder.lastAnalyzedAt;
    }

    public Long getId() { return id; }
    public String getProjectSlug() { return projectSlug; }
    public String getProvider() { return provider; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public LocalDateTime getLastAnalyzedAt() { return lastAnalyzedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String projectSlug;
        private String provider;
        private LocalDateTime addedAt;
        private LocalDateTime lastAnalyzedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder projectSlug(String projectSlug) { this.projectSlug = projectSlug; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder addedAt(LocalDateTime addedAt) { this.addedAt = addedAt; return this; }
        public Builder lastAnalyzedAt(LocalDateTime lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; return this; }

        public SavedRepository build() {
            return new SavedRepository(this);
        }
    }
}
