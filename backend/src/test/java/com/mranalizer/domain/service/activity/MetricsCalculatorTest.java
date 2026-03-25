package com.mranalizer.domain.service.activity;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.*;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    private MetricsCalculator calculator;
    private static final LocalDate REF_DATE = LocalDate.of(2026, 3, 25);

    @BeforeEach
    void setUp() {
        calculator = new MetricsCalculator();
    }

    // --- Helpers ---

    private MergeRequest mergedPr(String id, String author, LocalDateTime created,
                                   LocalDateTime merged, int additions, int deletions) {
        return MergeRequest.builder()
                .externalId(id)
                .author(author)
                .state("merged")
                .createdAt(created)
                .mergedAt(merged)
                .updatedAt(merged)
                .diffStats(new DiffStats(additions, deletions, 1))
                .build();
    }

    private MergeRequest openPr(String id, String author, int additions, int deletions) {
        return MergeRequest.builder()
                .externalId(id)
                .author(author)
                .state("open")
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .diffStats(new DiffStats(additions, deletions, 1))
                .build();
    }

    // --- Velocity ---

    @Nested
    class VelocityTests {

        @Test
        void happyPath_8PrsIn4Weeks() {
            LocalDateTime w1 = REF_DATE.minusWeeks(4).atTime(10, 0);
            LocalDateTime w2 = REF_DATE.minusWeeks(3).atTime(10, 0);
            LocalDateTime w3 = REF_DATE.minusWeeks(2).atTime(10, 0);
            LocalDateTime w4 = REF_DATE.minusWeeks(1).atTime(10, 0);

            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", w1.minusHours(2), w1, 10, 5),
                    mergedPr("2", "a", w1.minusHours(1), w1.plusHours(1), 10, 5),
                    mergedPr("3", "a", w2.minusHours(2), w2, 10, 5),
                    mergedPr("4", "a", w2.minusHours(1), w2.plusHours(1), 10, 5),
                    mergedPr("5", "a", w3.minusHours(2), w3, 10, 5),
                    mergedPr("6", "a", w3.minusHours(1), w3.plusHours(1), 10, 5),
                    mergedPr("7", "a", w4.minusHours(2), w4, 10, 5),
                    mergedPr("8", "a", w4.minusHours(1), w4.plusHours(1), 10, 5)
            );

            VelocityMetrics v = calculator.calculateVelocity(prs, REF_DATE);
            assertEquals(2.0, v.prsPerWeek(), 0.01);
            assertEquals(4, v.weeklyBreakdown().size());
            assertEquals("stable", v.trend());
        }

        @Test
        void zeroPrs() {
            VelocityMetrics v = calculator.calculateVelocity(List.of(), REF_DATE);
            assertEquals(0.0, v.prsPerWeek());
            assertEquals("stable", v.trend());
        }

        @Test
        void unevenDistribution_trendFalling() {
            LocalDateTime w1 = REF_DATE.minusWeeks(4).atTime(10, 0);

            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", w1.minusHours(6), w1, 10, 5),
                    mergedPr("2", "a", w1.minusHours(5), w1.plusHours(1), 10, 5),
                    mergedPr("3", "a", w1.minusHours(4), w1.plusHours(2), 10, 5),
                    mergedPr("4", "a", w1.minusHours(3), w1.plusHours(3), 10, 5),
                    mergedPr("5", "a", w1.minusHours(2), w1.plusHours(4), 10, 5),
                    mergedPr("6", "a", w1.minusHours(1), w1.plusHours(5), 10, 5)
            );

            VelocityMetrics v = calculator.calculateVelocity(prs, REF_DATE);
            assertEquals(1.5, v.prsPerWeek(), 0.01);
            assertEquals("falling", v.trend()); // last week = 0, avg = 1.5
        }

        @Test
        void trendRising() {
            LocalDateTime lastWeek = REF_DATE.minusWeeks(1).atTime(10, 0);

            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", lastWeek.minusHours(5), lastWeek, 10, 5),
                    mergedPr("2", "a", lastWeek.minusHours(4), lastWeek.plusHours(1), 10, 5),
                    mergedPr("3", "a", lastWeek.minusHours(3), lastWeek.plusHours(2), 10, 5),
                    mergedPr("4", "a", lastWeek.minusHours(2), lastWeek.plusHours(3), 10, 5),
                    mergedPr("5", "a", lastWeek.minusHours(1), lastWeek.plusHours(4), 10, 5)
            );

            VelocityMetrics v = calculator.calculateVelocity(prs, REF_DATE);
            assertEquals("rising", v.trend()); // last week = 5, avg = 1.25
        }
    }

    // --- Cycle Time ---

    @Nested
    class CycleTimeTests {

        @Test
        void happyPath() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(10, 0), ldt(11, 0), 10, 5),   // 1h
                    mergedPr("2", "a", ldt(10, 0), ldt(12, 0), 10, 5),   // 2h
                    mergedPr("3", "a", ldt(10, 0), ldt(13, 0), 10, 5),   // 3h
                    mergedPr("4", "a", ldt(10, 0), ldt(10, 0).plusHours(24), 10, 5),  // 24h
                    mergedPr("5", "a", ldt(10, 0), ldt(10, 0).plusHours(48), 10, 5)   // 48h
            );

            CycleTimeMetrics ct = calculator.calculateCycleTime(prs);
            assertEquals(15.6, ct.avgHours(), 0.1);
            assertEquals(3.0, ct.medianHours(), 0.1);
            assertEquals(48.0, ct.p90Hours(), 0.1);
        }

        @Test
        void singlePr() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(10, 0), ldt(12, 0), 10, 5)
            );

            CycleTimeMetrics ct = calculator.calculateCycleTime(prs);
            assertEquals(2.0, ct.avgHours(), 0.1);
            assertEquals(2.0, ct.medianHours(), 0.1);
            assertEquals(2.0, ct.p90Hours(), 0.1);
        }

        @Test
        void noMergedPrs() {
            CycleTimeMetrics ct = calculator.calculateCycleTime(List.of());
            assertEquals(0, ct.avgHours());
            assertEquals(0, ct.medianHours());
            assertEquals(0, ct.p90Hours());
        }

        private LocalDateTime ldt(int hour, int min) {
            return LocalDateTime.of(2026, 3, 20, hour, min);
        }
    }

    // --- Impact ---

    @Nested
    class ImpactTests {

        @Test
        void happyPath() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 100, 50),
                    mergedPr("2", "a", ldt(), ldt(), 200, 100),
                    mergedPr("3", "a", ldt(), ldt(), 500, 200)
            );

            ImpactMetrics im = calculator.calculateImpact(prs);
            assertEquals(800, im.totalAdditions());
            assertEquals(350, im.totalDeletions());
            assertEquals(1150, im.totalLines());
            assertEquals(383.33, im.avgLinesPerPr(), 0.5);
            assertEquals(2.285, im.addDeleteRatio(), 0.01);
        }

        @Test
        void zeroPrs() {
            ImpactMetrics im = calculator.calculateImpact(List.of());
            assertEquals(0, im.totalLines());
            assertEquals(0, im.avgLinesPerPr());
        }

        @Test
        void zeroDeletions() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 500, 0)
            );

            ImpactMetrics im = calculator.calculateImpact(prs);
            assertEquals(500, im.addDeleteRatio(), 0.01); // 500 / max(0,1)
        }

        private LocalDateTime ldt() {
            return LocalDateTime.of(2026, 3, 20, 10, 0);
        }
    }

    // --- Code Churn ---

    @Nested
    class ChurnTests {

        @Test
        void mainlyNewCode() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 1000, 50)
            );
            CodeChurnMetrics cm = calculator.calculateChurn(prs);
            assertEquals(0.05, cm.churnRatio(), 0.01);
            assertEquals("Głównie nowy kod", cm.label());
        }

        @Test
        void balanced() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 1000, 500)
            );
            CodeChurnMetrics cm = calculator.calculateChurn(prs);
            assertEquals(0.5, cm.churnRatio(), 0.01);
            assertEquals("Zbalansowany", cm.label());
        }

        @Test
        void heavyRefactoring() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 100, 900)
            );
            CodeChurnMetrics cm = calculator.calculateChurn(prs);
            assertEquals(9.0, cm.churnRatio(), 0.01);
            assertEquals("Przewaga refaktoringu", cm.label());
        }

        @Test
        void zeroAdditions() {
            List<MergeRequest> prs = List.of(
                    mergedPr("1", "a", ldt(), ldt(), 0, 100)
            );
            CodeChurnMetrics cm = calculator.calculateChurn(prs);
            assertEquals(100.0, cm.churnRatio(), 0.01); // 100 / max(0,1) = 100
        }

        @Test
        void zeroPrs() {
            CodeChurnMetrics cm = calculator.calculateChurn(List.of());
            assertEquals(0.0, cm.churnRatio());
            assertEquals("Głównie nowy kod", cm.label());
        }

        private LocalDateTime ldt() {
            return LocalDateTime.of(2026, 3, 20, 10, 0);
        }
    }

    // --- Review Engagement ---

    @Nested
    class ReviewEngagementTests {

        @Test
        void happyPath() {
            // alice authored PR 1, 2. bob authored PR 3.
            MergeRequest pr1 = mergedPr("1", "alice", ldt(), ldt(), 10, 5);
            MergeRequest pr2 = mergedPr("2", "alice", ldt(), ldt(), 10, 5);
            MergeRequest pr3 = mergedPr("3", "bob", ldt(), ldt(), 10, 5);

            Map<String, List<ReviewInfo>> reviews = Map.of(
                    "1", List.of(new ReviewInfo("bob", "APPROVED", ldt()),
                                 new ReviewInfo("carol", "COMMENTED", ldt())),
                    "2", List.of(new ReviewInfo("bob", "APPROVED", ldt())),
                    "3", List.of(new ReviewInfo("alice", "APPROVED", ldt()))
            );

            ReviewEngagementMetrics re = calculator.calculateReviewEngagement(
                    "alice", List.of(pr1, pr2, pr3), reviews);

            assertEquals(1, re.reviewsGiven());    // alice reviewed pr3
            assertEquals(3, re.reviewsReceived());  // bob+carol on pr1, bob on pr2
            assertEquals(0.33, re.ratio(), 0.01);
            assertEquals("Mało review", re.label());
        }

        @Test
        void noReviews() {
            MergeRequest pr1 = mergedPr("1", "alice", ldt(), ldt(), 10, 5);
            ReviewEngagementMetrics re = calculator.calculateReviewEngagement(
                    "alice", List.of(pr1), Map.of());

            assertEquals(0, re.reviewsGiven());
            assertEquals(0, re.reviewsReceived());
            assertEquals("Brak danych", re.label());
        }

        @Test
        void selfReviewsExcluded() {
            MergeRequest pr1 = mergedPr("1", "alice", ldt(), ldt(), 10, 5);

            Map<String, List<ReviewInfo>> reviews = Map.of(
                    "1", List.of(new ReviewInfo("alice", "APPROVED", ldt())) // self-review
            );

            ReviewEngagementMetrics re = calculator.calculateReviewEngagement(
                    "alice", List.of(pr1), reviews);

            assertEquals(0, re.reviewsGiven());
            assertEquals(0, re.reviewsReceived()); // self-review excluded
            assertEquals("Brak danych", re.label());
        }

        @Test
        void activeReviewer() {
            MergeRequest pr1 = mergedPr("1", "alice", ldt(), ldt(), 10, 5);
            MergeRequest pr2 = mergedPr("2", "bob", ldt(), ldt(), 10, 5);
            MergeRequest pr3 = mergedPr("3", "bob", ldt(), ldt(), 10, 5);
            MergeRequest pr4 = mergedPr("4", "carol", ldt(), ldt(), 10, 5);

            Map<String, List<ReviewInfo>> reviews = Map.of(
                    "1", List.of(new ReviewInfo("bob", "APPROVED", ldt())),
                    "2", List.of(new ReviewInfo("alice", "APPROVED", ldt())),
                    "3", List.of(new ReviewInfo("alice", "APPROVED", ldt())),
                    "4", List.of(new ReviewInfo("alice", "APPROVED", ldt()))
            );

            ReviewEngagementMetrics re = calculator.calculateReviewEngagement(
                    "alice", List.of(pr1, pr2, pr3, pr4), reviews);

            assertEquals(3, re.reviewsGiven());
            assertEquals(1, re.reviewsReceived());
            assertEquals(3.0, re.ratio(), 0.01);
            assertEquals("Aktywny reviewer", re.label());
        }

        private LocalDateTime ldt() {
            return LocalDateTime.of(2026, 3, 20, 10, 0);
        }
    }
}
