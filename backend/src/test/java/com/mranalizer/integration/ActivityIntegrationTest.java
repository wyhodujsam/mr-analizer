package com.mranalizer.integration;

import com.mranalizer.adapter.in.rest.activity.dto.ActivityReportResponse;
import com.mranalizer.adapter.in.rest.activity.dto.ContributorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityIntegrationTest extends GitHubIntegrationTestBase {

    @BeforeEach
    void setupRoutes() {
        // Map.of has 10-entry limit, use Map.ofEntries for more
        setupDispatcher(Map.ofEntries(
                Map.entry("/repos/test/repo/pulls\\?.*", "pr-list-3.json"),
                Map.entry("/repos/test/repo/pulls/1$", "pr-single-1.json"),
                Map.entry("/repos/test/repo/pulls/2$", "pr-single-2.json"),
                Map.entry("/repos/test/repo/pulls/3$", "pr-single-3.json"),
                Map.entry("/repos/test/repo/pulls/1/files.*", "pr-files-1.json"),
                Map.entry("/repos/test/repo/pulls/2/files.*", "pr-files-2.json"),
                Map.entry("/repos/test/repo/pulls/3/files.*", "pr-files-3.json"),
                Map.entry("/repos/test/repo/pulls/1/reviews.*", "pr-reviews-approved.json"),
                Map.entry("/repos/test/repo/pulls/2/reviews.*", "pr-reviews-empty.json"),
                Map.entry("/repos/test/repo/pulls/3/reviews.*", "pr-reviews-approved.json")
        ));
    }

    @Test
    void activityReport_hasNonZeroAvgSize_fromSinglePrEndpoint() {
        // This test catches the bug: browse returns 0 for additions/deletions
        // but activity should fetch single PR details with real sizes
        ResponseEntity<ActivityReportResponse> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice",
                ActivityReportResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ActivityReportResponse report = response.getBody();
        assertThat(report).isNotNull();
        assertThat(report.stats().totalPrs()).isEqualTo(2); // alice has PR #1 and #2
        assertThat(report.stats().avgSize())
                .as("avgSize must be > 0 (fetched from single PR endpoint, not list)")
                .isGreaterThan(0);
    }

    @Test
    void contributors_returnsUniqueAuthors() {
        ResponseEntity<List<ContributorResponse>> response = restTemplate.exchange(
                "/api/activity/test/repo/contributors",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ContributorResponse> contributors = response.getBody();
        assertThat(contributors).hasSize(2); // alice (2 PRs) + bob (1 PR)
        assertThat(contributors.get(0).login()).isEqualTo("alice"); // sorted by count desc
        assertThat(contributors.get(0).prCount()).isEqualTo(2);
    }

    @Test
    void activityReport_detectsNoReview() {
        // PR #2 has empty reviews → should flag "Brak review"
        ResponseEntity<ActivityReportResponse> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice",
                ActivityReportResponse.class);

        assertThat(response.getBody().flags()).anyMatch(f ->
                f.type().equals("NO_REVIEW") && f.prReference().equals("#2"));
    }

    @Test
    void activityReport_detectsQuickMerge() {
        // PR #2: created 09:00, merged 09:02 → 2 min with 1100 lines → critical quick merge
        ResponseEntity<ActivityReportResponse> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice",
                ActivityReportResponse.class);

        assertThat(response.getBody().flags()).anyMatch(f ->
                f.type().equals("SUSPICIOUS_QUICK_MERGE") && f.severity().equals("CRITICAL"));
    }

    @Test
    void activityReport_hasDailyActivityForHeatmap() {
        ResponseEntity<ActivityReportResponse> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice",
                ActivityReportResponse.class);

        assertThat(response.getBody().dailyActivity()).isNotEmpty();
        // alice has PRs on 2026-03-10 and 2026-03-15
        assertThat(response.getBody().dailyActivity()).containsKey("2026-03-10");
        assertThat(response.getBody().dailyActivity()).containsKey("2026-03-15");
    }
}
