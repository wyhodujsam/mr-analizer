package com.mranalizer.domain.model.activity;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityRepoCache {

    private final String projectSlug;
    private final Map<String, MergeRequest> detailedPrs;
    private final Map<String, List<ReviewInfo>> reviewsByPr;
    private volatile LocalDateTime lastUpdated;
    private volatile Instant lastFetchedAt;
    private final Duration ttl;

    public ActivityRepoCache(String projectSlug,
                             Map<String, MergeRequest> detailedPrs,
                             Map<String, List<ReviewInfo>> reviewsByPr,
                             Duration ttl) {
        this.projectSlug = projectSlug;
        this.detailedPrs = new ConcurrentHashMap<>(detailedPrs);
        this.reviewsByPr = new ConcurrentHashMap<>(reviewsByPr);
        this.ttl = ttl;
        this.lastFetchedAt = Instant.now();
        this.lastUpdated = computeMaxUpdatedAt();
    }

    public boolean needsRefresh() {
        return Instant.now().isAfter(lastFetchedAt.plus(ttl));
    }

    public String getProjectSlug() { return projectSlug; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public Instant getLastFetchedAt() { return lastFetchedAt; }

    /** C2 fix: synchronized to ensure readers see consistent state after merge */
    public synchronized void mergeUpdatedPrs(Map<String, MergeRequest> updatedDetails,
                                              Map<String, List<ReviewInfo>> updatedReviews) {
        detailedPrs.putAll(updatedDetails);
        reviewsByPr.putAll(updatedReviews);
        lastUpdated = computeMaxUpdatedAt();
        lastFetchedAt = Instant.now();
    }

    public void touchLastFetched() {
        lastFetchedAt = Instant.now();
    }

    public synchronized List<MergeRequest> getPrsForAuthor(String author) {
        return detailedPrs.values().stream()
                .filter(mr -> author.equals(mr.getAuthor()))
                .toList();
    }

    public synchronized List<MergeRequest> getAllDetailedPrs() {
        return List.copyOf(detailedPrs.values());
    }

    public synchronized Map<String, List<ReviewInfo>> getReviewsByPr() {
        return Map.copyOf(reviewsByPr);
    }

    public int size() {
        return detailedPrs.size();
    }

    /** C3 fix: use LocalDateTime.MIN as fallback instead of null lastUpdated */
    private LocalDateTime computeMaxUpdatedAt() {
        return detailedPrs.values().stream()
                .map(MergeRequest::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
    }
}
