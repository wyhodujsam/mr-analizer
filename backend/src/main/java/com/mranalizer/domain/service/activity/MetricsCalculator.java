package com.mranalizer.domain.service.activity;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.*;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class MetricsCalculator {

    private static final int VELOCITY_WEEKS = 4;

    public VelocityMetrics calculateVelocity(List<MergeRequest> mergedPrs, LocalDate referenceDate) {
        // W5 fix: compute week bins first, then use their range as the window
        List<VelocityMetrics.WeeklyCount> weekly = new ArrayList<>();
        for (int w = VELOCITY_WEEKS; w >= 1; w--) {
            LocalDate weekStart = referenceDate.minusWeeks(w)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekly.add(new VelocityMetrics.WeeklyCount(weekStart, 0));
        }

        LocalDate windowStart = weekly.get(0).weekStart();
        LocalDate windowEnd = weekly.get(weekly.size() - 1).weekStart().plusDays(6);

        List<MergeRequest> recentMerged = mergedPrs.stream()
                .filter(mr -> mr.getMergedAt() != null)
                .filter(mr -> !mr.getMergedAt().toLocalDate().isBefore(windowStart))
                .filter(mr -> !mr.getMergedAt().toLocalDate().isAfter(windowEnd))
                .toList();

        // Count PRs per week bin
        List<VelocityMetrics.WeeklyCount> counted = new ArrayList<>();
        for (VelocityMetrics.WeeklyCount wc : weekly) {
            LocalDate ws = wc.weekStart();
            LocalDate we = ws.plusDays(6);
            int count = (int) recentMerged.stream()
                    .filter(mr -> {
                        LocalDate d = mr.getMergedAt().toLocalDate();
                        return !d.isBefore(ws) && !d.isAfter(we);
                    })
                    .count();
            counted.add(new VelocityMetrics.WeeklyCount(ws, count));
        }

        double prsPerWeek = recentMerged.size() / (double) VELOCITY_WEEKS;

        String trend = computeTrend(counted, prsPerWeek);

        return new VelocityMetrics(prsPerWeek, counted, trend);
    }

    public CycleTimeMetrics calculateCycleTime(List<MergeRequest> mergedPrs) {
        List<Double> hours = mergedPrs.stream()
                .filter(mr -> mr.getCreatedAt() != null && mr.getMergedAt() != null)
                .map(mr -> Duration.between(mr.getCreatedAt(), mr.getMergedAt()).toMinutes() / 60.0)
                .sorted()
                .toList();

        if (hours.isEmpty()) {
            return new CycleTimeMetrics(0, 0, 0);
        }

        double avg = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = percentile(hours, 0.5);
        double p90 = percentile(hours, 0.9);

        return new CycleTimeMetrics(avg, median, p90);
    }

    public ImpactMetrics calculateImpact(List<MergeRequest> prs) {
        if (prs.isEmpty()) {
            return new ImpactMetrics(0, 0, 0, 0, 0);
        }

        int totalAdd = prs.stream().mapToInt(mr -> mr.getDiffStats().additions()).sum();
        int totalDel = prs.stream().mapToInt(mr -> mr.getDiffStats().deletions()).sum();
        int totalLines = totalAdd + totalDel;
        double avg = (double) totalLines / prs.size();
        double ratio = (double) totalAdd / Math.max(totalDel, 1);

        return new ImpactMetrics(totalAdd, totalDel, totalLines, avg, ratio);
    }

    public CodeChurnMetrics calculateChurn(List<MergeRequest> prs) {
        int totalAdd = prs.stream().mapToInt(mr -> mr.getDiffStats().additions()).sum();
        int totalDel = prs.stream().mapToInt(mr -> mr.getDiffStats().deletions()).sum();

        double churnRatio = (double) totalDel / Math.max(totalAdd, 1);

        String label;
        if (churnRatio < 0.2) {
            label = "Głównie nowy kod";
        } else if (churnRatio <= 0.8) {
            label = "Zbalansowany";
        } else {
            label = "Przewaga refaktoringu";
        }

        return new CodeChurnMetrics(churnRatio, label);
    }

    public ReviewEngagementMetrics calculateReviewEngagement(
            String author,
            List<MergeRequest> allRepoPrs,
            Map<String, List<ReviewInfo>> allReviews) {

        int given = 0;
        int received = 0;

        for (MergeRequest mr : allRepoPrs) {
            List<ReviewInfo> reviews = allReviews.getOrDefault(mr.getExternalId(), List.of());
            boolean isAuthorPr = author.equals(mr.getAuthor());

            for (ReviewInfo review : reviews) {
                if (author.equals(review.reviewer())) continue; // skip self-reviews
                if (isAuthorPr) {
                    received++;
                }
            }
            if (!isAuthorPr) {
                for (ReviewInfo review : reviews) {
                    if (author.equals(review.reviewer())) {
                        given++;
                    }
                }
            }
        }

        double ratio = (double) given / Math.max(received, 1);

        String label;
        if (given == 0 && received == 0) {
            label = "Brak danych";
        } else if (ratio > 1.5) {
            label = "Aktywny reviewer";
        } else if (ratio >= 0.5) {
            label = "Zbalansowany";
        } else {
            label = "Mało review";
        }

        return new ReviewEngagementMetrics(given, received, ratio, label);
    }

    public ProductivityMetrics calculateAll(
            String author,
            List<MergeRequest> userPrs,
            List<MergeRequest> allRepoPrs,
            Map<String, List<ReviewInfo>> allReviews) {
        return calculateAll(author, userPrs, allRepoPrs, allReviews, LocalDate.now());
    }

    public ProductivityMetrics calculateAll(
            String author,
            List<MergeRequest> userPrs,
            List<MergeRequest> allRepoPrs,
            Map<String, List<ReviewInfo>> allReviews,
            LocalDate referenceDate) {

        List<MergeRequest> mergedPrs = userPrs.stream()
                .filter(mr -> mr.getMergedAt() != null)
                .toList();

        VelocityMetrics velocity = calculateVelocity(mergedPrs, referenceDate);
        CycleTimeMetrics cycleTime = calculateCycleTime(mergedPrs);
        ImpactMetrics impact = calculateImpact(userPrs);
        CodeChurnMetrics churn = calculateChurn(userPrs);
        ReviewEngagementMetrics engagement = calculateReviewEngagement(author, allRepoPrs, allReviews);

        return new ProductivityMetrics(velocity, cycleTime, impact, churn, engagement);
    }

    private String computeTrend(List<VelocityMetrics.WeeklyCount> weekly, double avg) {
        if (weekly.isEmpty() || avg == 0) return "stable";

        int lastWeekCount = weekly.get(weekly.size() - 1).count();
        if (lastWeekCount > avg * 1.2) return "rising";
        if (lastWeekCount < avg * 0.8) return "falling";
        return "stable";
    }

    private double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        if (sorted.size() == 1) return sorted.get(0);
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
}
