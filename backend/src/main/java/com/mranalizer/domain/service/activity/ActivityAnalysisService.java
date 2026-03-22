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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActivityAnalysisService implements ActivityAnalysisUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivityAnalysisService.class);

    private final MergeRequestProvider mergeRequestProvider;
    private final ReviewProvider reviewProvider;
    private final List<ActivityRule> rules;
    private final AggregateRules aggregateRules;

    public ActivityAnalysisService(MergeRequestProvider mergeRequestProvider,
                                   ReviewProvider reviewProvider,
                                   List<ActivityRule> rules,
                                   AggregateRules aggregateRules) {
        this.mergeRequestProvider = mergeRequestProvider;
        this.reviewProvider = reviewProvider;
        this.rules = rules;
        this.aggregateRules = aggregateRules;
    }

    @Override
    public List<ContributorInfo> getContributors(String projectSlug) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(projectSlug)
                .state("all")
                .limit(200)
                .build();

        List<MergeRequest> allPrs = mergeRequestProvider.fetchMergeRequests(criteria);

        return allPrs.stream()
                .filter(mr -> mr.getAuthor() != null)
                .collect(Collectors.groupingBy(MergeRequest::getAuthor, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ContributorInfo(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(ContributorInfo::prCount).reversed())
                .toList();
    }

    @Override
    public ActivityReport analyzeActivity(String projectSlug, String author) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(projectSlug)
                .state("all")
                .limit(200)
                .build();

        List<MergeRequest> allPrs = mergeRequestProvider.fetchMergeRequests(criteria);
        List<MergeRequest> userPrsBrowse = allPrs.stream()
                .filter(mr -> author.equals(mr.getAuthor()))
                .toList();

        if (userPrsBrowse.isEmpty()) {
            return emptyReport(author, projectSlug);
        }

        // Parallel fetch: PR details + reviews
        List<MergeRequest> userPrs = fetchDetailsParallel(projectSlug, userPrsBrowse);
        Map<String, List<ReviewInfo>> reviewsByPr = fetchReviewsParallel(projectSlug, userPrs);

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

        // Sort flags: critical first
        allFlags.sort(Comparator.comparing(ActivityFlag::severity));

        ContributorStats stats = buildStats(userPrs, allFlags);
        Map<LocalDate, DailyActivity> dailyActivity = buildDailyActivity(userPrs, flagsByPr);

        return new ActivityReport(author, projectSlug, stats, allFlags, dailyActivity, userPrs);
    }

    private List<MergeRequest> fetchDetailsParallel(String projectSlug, List<MergeRequest> browsePrs) {
        List<CompletableFuture<MergeRequest>> futures = browsePrs.stream()
                .map(mr -> CompletableFuture.supplyAsync(() -> fetchDetailSafe(projectSlug, mr)))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private Map<String, List<ReviewInfo>> fetchReviewsParallel(String projectSlug, List<MergeRequest> prs) {
        Map<String, List<ReviewInfo>> result = new ConcurrentHashMap<>();

        // Only fetch reviews for merged PRs (saves API calls)
        List<MergeRequest> mergedPrs = prs.stream()
                .filter(mr -> "merged".equals(mr.getState()))
                .toList();

        List<CompletableFuture<Void>> futures = mergedPrs.stream()
                .map(mr -> CompletableFuture.runAsync(() -> {
                    List<ReviewInfo> reviews = fetchReviewsSafe(projectSlug, mr);
                    result.put(mr.getExternalId(), reviews);
                }))
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

    private ContributorStats buildStats(List<MergeRequest> prs, List<ActivityFlag> flags) {
        double avgSize = prs.stream()
                .mapToInt(mr -> mr.getDiffStats().additions() + mr.getDiffStats().deletions())
                .average()
                .orElse(0);

        double avgReviewTime = prs.stream()
                .filter(mr -> mr.getCreatedAt() != null && mr.getMergedAt() != null)
                .mapToLong(mr -> Duration.between(mr.getCreatedAt(), mr.getMergedAt()).toMinutes())
                .average()
                .orElse(0);

        // Weekend percentage: only count PRs with known createdAt
        List<MergeRequest> prsWithDate = prs.stream()
                .filter(mr -> mr.getCreatedAt() != null)
                .toList();
        long weekendCount = prsWithDate.stream()
                .filter(mr -> DateTimeUtils.isWeekend(mr.getCreatedAt()))
                .count();
        double weekendPct = prsWithDate.isEmpty() ? 0 : (double) weekendCount / prsWithDate.size() * 100;

        Map<Severity, Long> flagCounts = flags.stream()
                .collect(Collectors.groupingBy(ActivityFlag::severity, Collectors.counting()));

        return new ContributorStats(prs.size(), avgSize, avgReviewTime, weekendPct, flagCounts);
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
