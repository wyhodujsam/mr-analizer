package com.mranalizer.domain.service.activity;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.*;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.service.activity.rules.ActivityRule;
import com.mranalizer.domain.service.activity.rules.AggregateRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ActivityAnalysisService implements ActivityAnalysisUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivityAnalysisService.class);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_INCREMENTAL_PRS = 500;

    private final MergeRequestProvider mergeRequestProvider;
    private final ReviewProvider reviewProvider;
    private final List<ActivityRule> rules;
    private final AggregateRules aggregateRules;
    private final MetricsCalculator metricsCalculator;
    private final Duration cacheTtl;
    private final ExecutorService fetchExecutor = Executors.newFixedThreadPool(8);

    private final ConcurrentHashMap<String, ActivityRepoCache> repoCache = new ConcurrentHashMap<>();

    public ActivityAnalysisService(MergeRequestProvider mergeRequestProvider,
                                   ReviewProvider reviewProvider,
                                   List<ActivityRule> rules,
                                   AggregateRules aggregateRules,
                                   MetricsCalculator metricsCalculator) {
        this(mergeRequestProvider, reviewProvider, rules, aggregateRules, metricsCalculator, DEFAULT_CACHE_TTL);
    }

    public ActivityAnalysisService(MergeRequestProvider mergeRequestProvider,
                                   ReviewProvider reviewProvider,
                                   List<ActivityRule> rules,
                                   AggregateRules aggregateRules,
                                   MetricsCalculator metricsCalculator,
                                   Duration cacheTtl) {
        this.mergeRequestProvider = mergeRequestProvider;
        this.reviewProvider = reviewProvider;
        this.rules = rules;
        this.aggregateRules = aggregateRules;
        this.metricsCalculator = metricsCalculator;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public List<ContributorInfo> getContributors(String projectSlug) {
        ActivityRepoCache cache = getOrFetchCache(projectSlug);

        return cache.getAllDetailedPrs().stream()
                .filter(mr -> mr.getAuthor() != null)
                .collect(Collectors.groupingBy(MergeRequest::getAuthor, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ContributorInfo(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(ContributorInfo::prCount).reversed())
                .toList();
    }

    @Override
    public ActivityReport analyzeActivity(String projectSlug, String author) {
        ActivityRepoCache cache = getOrFetchCache(projectSlug);
        List<MergeRequest> userPrs = cache.getPrsForAuthor(author);

        if (userPrs.isEmpty()) {
            return emptyReport(author, projectSlug);
        }

        Map<String, List<ReviewInfo>> reviewsByPr = cache.getReviewsByPr();

        // Evaluate per-PR rules
        List<ActivityFlag> allFlags = new ArrayList<>();
        Map<String, List<ActivityFlag>> flagsByPr = new HashMap<>();

        for (MergeRequest mr : userPrs) {
            List<ReviewInfo> reviews = reviewsByPr.getOrDefault(mr.getExternalId(), List.of());
            List<ActivityFlag> prFlags = new ArrayList<>();

            for (ActivityRule rule : rules) {
                prFlags.addAll(rule.evaluate(mr, reviews));
            }

            allFlags.addAll(prFlags);
            flagsByPr.put(mr.getExternalId(), prFlags);
        }

        // Aggregate rules
        allFlags.addAll(aggregateRules.evaluate(userPrs));
        allFlags.sort(Comparator.comparing(ActivityFlag::severity));

        // Productivity metrics
        ProductivityMetrics productivity = metricsCalculator.calculateAll(
                author, userPrs, cache.getAllDetailedPrs(), reviewsByPr);

        ContributorStats stats = buildStats(userPrs, allFlags, productivity);
        Map<LocalDate, DailyActivity> dailyActivity = buildDailyActivity(userPrs, flagsByPr);

        return new ActivityReport(author, projectSlug, stats, allFlags, dailyActivity, userPrs);
    }

    @Override
    public void invalidateCache(String projectSlug) {
        repoCache.remove(projectSlug);
        log.info("Activity cache INVALIDATED for {}", projectSlug);
    }

    @Override
    public void refreshCache(String projectSlug) {
        ActivityRepoCache cached = repoCache.get(projectSlug);
        if (cached != null) {
            incrementalUpdate(projectSlug, cached);
        }
    }

    // --- Cache logic (C1 fix: computeIfAbsent for cold start, synchronized for incremental) ---

    ActivityRepoCache getOrFetchCache(String projectSlug) {
        ActivityRepoCache cached = repoCache.computeIfAbsent(projectSlug, slug -> {
            log.info("Activity cache MISS for {} — full fetch", slug);
            return fullFetch(slug);
        });

        if (cached.needsRefresh()) {
            synchronized (cached) {
                // Double-check after acquiring lock
                if (cached.needsRefresh()) {
                    log.info("Activity cache STALE for {} — incremental update since {}",
                            projectSlug, cached.getLastUpdated());
                    incrementalUpdate(projectSlug, cached);
                }
            }
        }

        return cached;
    }

    private ActivityRepoCache fullFetch(String projectSlug) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(projectSlug)
                .state("all")
                .limit(200)
                .build();

        List<MergeRequest> browsePrs = mergeRequestProvider.fetchMergeRequests(criteria);
        Map<String, MergeRequest> details = fetchAllDetailsParallel(projectSlug, browsePrs);
        Map<String, List<ReviewInfo>> reviews = fetchAllReviewsParallel(projectSlug, details.values());

        return new ActivityRepoCache(projectSlug, details, reviews, cacheTtl);
    }

    private void incrementalUpdate(String projectSlug, ActivityRepoCache cache) {
        LocalDateTime since = cache.getLastUpdated();

        List<MergeRequest> updatedBrowse =
                mergeRequestProvider.fetchMergeRequestsUpdatedSince(projectSlug, since);

        if (updatedBrowse.isEmpty()) {
            cache.touchLastFetched();
            log.info("Incremental update for {}: 0 changed PRs", projectSlug);
            return;
        }

        // W7 fix: safety limit on incremental fetch
        if (updatedBrowse.size() > MAX_INCREMENTAL_PRS) {
            log.warn("Incremental update for {}: {} changed PRs exceeds limit {}, doing full fetch",
                    projectSlug, updatedBrowse.size(), MAX_INCREMENTAL_PRS);
            ActivityRepoCache fresh = fullFetch(projectSlug);
            repoCache.put(projectSlug, fresh);
            return;
        }

        log.info("Incremental update for {}: {} changed PRs", projectSlug, updatedBrowse.size());

        Map<String, MergeRequest> updatedDetails = fetchAllDetailsParallel(projectSlug, updatedBrowse);
        Map<String, List<ReviewInfo>> updatedReviews = fetchAllReviewsParallel(projectSlug, updatedDetails.values());

        cache.mergeUpdatedPrs(updatedDetails, updatedReviews);
    }

    // --- Parallel fetch helpers (W1 fix: bounded executor instead of ForkJoinPool) ---

    private Map<String, MergeRequest> fetchAllDetailsParallel(String projectSlug, Collection<MergeRequest> browsePrs) {
        Map<String, MergeRequest> result = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = browsePrs.stream()
                .map(mr -> CompletableFuture.runAsync(() -> {
                    MergeRequest detail = fetchDetailSafe(projectSlug, mr);
                    result.put(detail.getExternalId(), detail);
                }, fetchExecutor))
                .toList();

        futures.forEach(CompletableFuture::join);
        return result;
    }

    private Map<String, List<ReviewInfo>> fetchAllReviewsParallel(String projectSlug, Collection<MergeRequest> prs) {
        Map<String, List<ReviewInfo>> result = new ConcurrentHashMap<>();

        List<MergeRequest> mergedPrs = prs.stream()
                .filter(mr -> "merged".equals(mr.getState()))
                .toList();

        List<CompletableFuture<Void>> futures = mergedPrs.stream()
                .map(mr -> CompletableFuture.runAsync(() -> {
                    List<ReviewInfo> reviews = fetchReviewsSafe(projectSlug, mr);
                    result.put(mr.getExternalId(), reviews);
                }, fetchExecutor))
                .toList();

        futures.forEach(CompletableFuture::join);
        return result;
    }

    private MergeRequest fetchDetailSafe(String projectSlug, MergeRequest browseMr) {
        try {
            return mergeRequestProvider.fetchMergeRequest(projectSlug, browseMr.getExternalId());
        } catch (Exception e) {
            log.warn("Failed to fetch details for PR #{}, using browse data: {}",
                    browseMr.getExternalId(), e.getMessage());
            return browseMr;
        }
    }

    private List<ReviewInfo> fetchReviewsSafe(String projectSlug, MergeRequest mr) {
        try {
            if (mr.getExternalId() == null) return List.of();
            return reviewProvider.fetchReviews(projectSlug, mr.getExternalId());
        } catch (Exception e) {
            log.warn("Failed to fetch reviews for PR #{}: {}", mr.getExternalId(), e.getMessage());
            return List.of();
        }
    }

    // --- Stats building ---

    private ContributorStats buildStats(List<MergeRequest> prs, List<ActivityFlag> flags,
                                         ProductivityMetrics productivity) {
        double avgSize = prs.stream()
                .mapToInt(mr -> mr.getDiffStats().additions() + mr.getDiffStats().deletions())
                .average()
                .orElse(0);

        double avgReviewTime = prs.stream()
                .filter(mr -> mr.getCreatedAt() != null && mr.getMergedAt() != null)
                .mapToLong(mr -> Duration.between(mr.getCreatedAt(), mr.getMergedAt()).toMinutes())
                .average()
                .orElse(0);

        List<MergeRequest> prsWithDate = prs.stream()
                .filter(mr -> mr.getCreatedAt() != null)
                .toList();
        long weekendCount = prsWithDate.stream()
                .filter(mr -> DateTimeUtils.isWeekend(mr.getCreatedAt()))
                .count();
        double weekendPct = prsWithDate.isEmpty() ? 0 : (double) weekendCount / prsWithDate.size() * 100;

        Map<Severity, Long> flagCounts = flags.stream()
                .collect(Collectors.groupingBy(ActivityFlag::severity, Collectors.counting()));

        return new ContributorStats(prs.size(), avgSize, avgReviewTime, weekendPct, flagCounts, productivity);
    }

    private Map<LocalDate, DailyActivity> buildDailyActivity(List<MergeRequest> prs,
                                                              Map<String, List<ActivityFlag>> flagsByPr) {
        Map<LocalDate, List<MergeRequest>> byDate = prs.stream()
                .filter(mr -> mr.getCreatedAt() != null)
                .collect(Collectors.groupingBy(mr -> mr.getCreatedAt().toLocalDate()));

        Map<LocalDate, DailyActivity> result = new TreeMap<>();
        for (var entry : byDate.entrySet()) {
            List<DailyActivity.PrSummary> summaries = entry.getValue().stream()
                    .map(mr -> new DailyActivity.PrSummary(
                            mr.getExternalId(),
                            mr.getTitle(),
                            mr.getDiffStats().additions() + mr.getDiffStats().deletions(),
                            flagsByPr.getOrDefault(mr.getExternalId(), List.of())))
                    .toList();
            result.put(entry.getKey(), new DailyActivity(entry.getKey(), summaries.size(), summaries));
        }

        return result;
    }

    private ActivityReport emptyReport(String author, String projectSlug) {
        ContributorStats emptyStats = new ContributorStats(0, 0, 0, 0, Map.of());
        return new ActivityReport(author, projectSlug, emptyStats, List.of(), Map.of(), List.of());
    }
}
