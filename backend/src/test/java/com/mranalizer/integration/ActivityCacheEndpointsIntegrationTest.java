package com.mranalizer.integration;

import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCacheEndpointsIntegrationTest extends GitHubIntegrationTestBase {

    @Autowired
    private ActivityAnalysisUseCase activityAnalysis;

    @BeforeEach
    void setupRoutes() {
        activityAnalysis.invalidateCache("test/repo");
        setupDispatcher(Map.ofEntries(
                Map.entry("/repos/test/repo/pulls\\?.*", "pr-list-3.json"),
                Map.entry("/repos/test/repo/pulls/1$", "pr-single-1.json"),
                Map.entry("/repos/test/repo/pulls/2$", "pr-single-2.json"),
                Map.entry("/repos/test/repo/pulls/3$", "pr-single-3.json"),
                Map.entry("/repos/test/repo/pulls/1/reviews.*", "pr-reviews-approved.json"),
                Map.entry("/repos/test/repo/pulls/2/reviews.*", "pr-reviews-empty.json"),
                Map.entry("/repos/test/repo/pulls/3/reviews.*", "pr-reviews-approved.json")
        ));
    }

    @Test
    void refresh_returns204() {
        // Warm cache first
        restTemplate.getForEntity("/api/activity/test/repo/report?author=alice", String.class);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/activity/test/repo/refresh", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void invalidateCache_returns204() {
        // Warm cache first
        restTemplate.getForEntity("/api/activity/test/repo/report?author=alice", String.class);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/activity/test/repo/cache", HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void afterInvalidate_nextRequestFetchesFromApi() {
        // Warm cache
        restTemplate.getForEntity("/api/activity/test/repo/report?author=alice", String.class);

        // Invalidate
        restTemplate.exchange("/api/activity/test/repo/cache", HttpMethod.DELETE, null, Void.class);

        // Next request should work (fetches fresh from MockWebServer)
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
