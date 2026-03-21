package com.mranalizer.domain.model;

import com.mranalizer.domain.exception.InvalidRequestException;

import java.time.LocalDate;

/**
 * Criteria used to filter merge requests when fetching from a VCS provider.
 */
public class FetchCriteria {

    private static final int MAX_LIMIT = 200;

    private final String projectSlug;
    private final String targetBranch;
    private final String state;
    private final LocalDate after;
    private final LocalDate before;
    private final int limit;

    private FetchCriteria(Builder builder) {
        this.projectSlug = builder.projectSlug;
        this.targetBranch = builder.targetBranch;
        this.state = builder.state;
        this.after = builder.after;
        this.before = builder.before;
        this.limit = Math.min(builder.limit, MAX_LIMIT);
        validate();
    }

    private void validate() {
        if (after != null && before != null && after.isAfter(before)) {
            throw new InvalidRequestException("'after' date must be before 'before' date");
        }
    }

    public String getProjectSlug() { return projectSlug; }
    public String getTargetBranch() { return targetBranch; }
    public String getState() { return state; }
    public LocalDate getAfter() { return after; }
    public LocalDate getBefore() { return before; }
    public int getLimit() { return limit; }

    public String cacheKey() {
        return projectSlug + "|" +
               (state != null ? state : "") + "|" +
               (targetBranch != null ? targetBranch : "") + "|" +
               (after != null ? after : "") + "|" +
               (before != null ? before : "") + "|" +
               limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String projectSlug;
        private String targetBranch;
        private String state;
        private LocalDate after;
        private LocalDate before;
        private int limit = 100;

        private Builder() {}

        public Builder projectSlug(String projectSlug) { this.projectSlug = projectSlug; return this; }
        public Builder targetBranch(String targetBranch) { this.targetBranch = targetBranch; return this; }
        public Builder state(String state) { this.state = state; return this; }
        public Builder after(LocalDate after) { this.after = after; return this; }
        public Builder before(LocalDate before) { this.before = before; return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }

        public FetchCriteria build() {
            return new FetchCriteria(this);
        }
    }
}
