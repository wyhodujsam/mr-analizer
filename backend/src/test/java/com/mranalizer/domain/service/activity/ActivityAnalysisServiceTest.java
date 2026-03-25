package com.mranalizer.domain.service.activity;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.*;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import com.mranalizer.domain.service.activity.rules.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ActivityAnalysisServiceTest {

    private MergeRequestProvider mrProvider;
    private ReviewProvider reviewProvider;
    private ActivityAnalysisService service;

    @BeforeEach
    void setUp() {
        mrProvider = Mockito.mock(MergeRequestProvider.class);
        reviewProvider = Mockito.mock(ReviewProvider.class);
        List<ActivityRule> rules = List.of(
                new LargePrRule(),
                new QuickReviewRule(),
                new WeekendWorkRule(),
                new NightWorkRule(),
                new NoReviewRule(),
                new SelfMergeRule()
        );
        service = new ActivityAnalysisService(
                mrProvider, reviewProvider, rules, new AggregateRules(),
                new MetricsCalculator(), Duration.ofHours(1)); // long TTL so cache doesn't expire mid-test
    }

    private void setupFetchDetail(List<MergeRequest> prs) {
        for (MergeRequest mr : prs) {
            when(mrProvider.fetchMergeRequest(anyString(), eq(mr.getExternalId()))).thenReturn(mr);
        }
    }

    @Test
    void shouldReturnEmptyReportForUnknownAuthor() {
        when(mrProvider.fetchMergeRequests(any(FetchCriteria.class))).thenReturn(List.of());

        ActivityReport report = service.analyzeActivity("owner/repo", "ghost");

        assertEquals(0, report.getStats().totalPrs());
        assertTrue(report.getFlags().isEmpty());
        assertTrue(report.getDailyActivity().isEmpty());
    }

    @Test
    void shouldDetectLargePrs() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 600, 0, LocalDateTime.of(2026, 3, 9, 10, 0), null, "open"),
                buildMr("2", "jan", 100, 0, LocalDateTime.of(2026, 3, 10, 10, 0), null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);
        when(reviewProvider.fetchReviews(any(), anyString())).thenReturn(List.of());

        ActivityReport report = service.analyzeActivity("owner/repo", "jan");

        assertEquals(2, report.getStats().totalPrs());
        assertTrue(report.getFlags().stream()
                .anyMatch(f -> f.type() == FlagType.LARGE_PR));
    }

    @Test
    void shouldBuildDailyActivity() {
        LocalDateTime day1 = LocalDateTime.of(2026, 3, 2, 10, 0);
        LocalDateTime day1b = LocalDateTime.of(2026, 3, 2, 14, 0);
        LocalDateTime day2 = LocalDateTime.of(2026, 3, 5, 9, 0);

        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 50, 0, day1, null, "open"),
                buildMr("2", "jan", 30, 0, day1b, null, "open"),
                buildMr("3", "jan", 20, 0, day2, null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);
        when(reviewProvider.fetchReviews(any(), anyString())).thenReturn(List.of());

        ActivityReport report = service.analyzeActivity("owner/repo", "jan");

        DailyActivity march2 = report.getDailyActivity().get(LocalDate.of(2026, 3, 2));
        assertNotNull(march2);
        assertEquals(2, march2.count());

        DailyActivity march5 = report.getDailyActivity().get(LocalDate.of(2026, 3, 5));
        assertNotNull(march5);
        assertEquals(1, march5.count());
    }

    @Test
    void shouldCalculateStats() {
        LocalDateTime created = LocalDateTime.of(2026, 3, 9, 10, 0);
        LocalDateTime merged = LocalDateTime.of(2026, 3, 9, 11, 0); // 60 min

        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 200, 100, created, merged, "merged"),
                buildMr("2", "jan", 100, 50, created, merged, "merged")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);
        when(reviewProvider.fetchReviews(any(), anyString())).thenReturn(
                List.of(new ReviewInfo("other", "APPROVED", LocalDateTime.now())));

        ActivityReport report = service.analyzeActivity("owner/repo", "jan");

        assertEquals(2, report.getStats().totalPrs());
        assertEquals(225.0, report.getStats().avgSize()); // (300 + 150) / 2
        assertEquals(60.0, report.getStats().avgReviewTimeMinutes());
        assertEquals(0.0, report.getStats().weekendPercentage());
        assertNotNull(report.getStats().productivity());
    }

    @Test
    void shouldGetContributors() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 100, 0, LocalDateTime.now(), null, "open"),
                buildMr("2", "jan", 100, 0, LocalDateTime.now(), null, "open"),
                buildMr("3", "anna", 100, 0, LocalDateTime.now(), null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);

        List<ContributorInfo> contributors = service.getContributors("owner/repo");

        assertEquals(2, contributors.size());
        assertEquals("jan", contributors.get(0).login()); // most PRs first
        assertEquals(2, contributors.get(0).prCount());
        assertEquals("anna", contributors.get(1).login());
        assertEquals(1, contributors.get(1).prCount());
    }

    @Test
    void shouldSortFlagsBySeverity() {
        // Weekend (info) + large PR (warning) — should be sorted critical > warning > info
        LocalDateTime saturday = LocalDateTime.of(2026, 3, 7, 10, 0);
        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 600, 0, saturday, null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);
        when(reviewProvider.fetchReviews(any(), anyString())).thenReturn(List.of());

        ActivityReport report = service.analyzeActivity("owner/repo", "jan");

        assertTrue(report.getFlags().size() >= 2);
        // First flag should be WARNING (LARGE_PR), last INFO (WEEKEND_WORK)
        Severity first = report.getFlags().get(0).severity();
        Severity last = report.getFlags().get(report.getFlags().size() - 1).severity();
        assertTrue(first.ordinal() <= last.ordinal());
    }

    @Test
    void shouldUseCacheForSecondAuthor() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 100, 0, LocalDateTime.now(), null, "open"),
                buildMr("2", "anna", 100, 0, LocalDateTime.now(), null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);
        when(reviewProvider.fetchReviews(any(), anyString())).thenReturn(List.of());

        // First call — cache populated
        service.analyzeActivity("owner/repo", "jan");

        // Reset mock to verify no more calls
        Mockito.reset(mrProvider);

        // Second call — should use cache
        ActivityReport report = service.analyzeActivity("owner/repo", "anna");

        assertEquals(1, report.getStats().totalPrs());
        Mockito.verifyNoInteractions(mrProvider);
    }

    @Test
    void shouldInvalidateCache() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "jan", 100, 0, LocalDateTime.now(), null, "open")
        );
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        setupFetchDetail(prs);

        service.analyzeActivity("owner/repo", "jan");

        service.invalidateCache("owner/repo");

        // After invalidation, next call should fetch again
        service.analyzeActivity("owner/repo", "jan");
        Mockito.verify(mrProvider, Mockito.times(2)).fetchMergeRequests(any());
    }

    private MergeRequest buildMr(String id, String author, int additions, int deletions,
                                  LocalDateTime createdAt, LocalDateTime mergedAt, String state) {
        return MergeRequest.builder()
                .externalId(id)
                .title("PR #" + id)
                .author(author)
                .state(state)
                .diffStats(new DiffStats(additions, deletions, 3))
                .createdAt(createdAt)
                .mergedAt(mergedAt)
                .updatedAt(createdAt)
                .build();
    }
}
