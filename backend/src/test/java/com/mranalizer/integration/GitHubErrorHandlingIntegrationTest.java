package com.mranalizer.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubErrorHandlingIntegrationTest extends GitHubIntegrationTestBase {

    private HttpEntity<Map<String, Object>> browseRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "projectSlug", "test/repo",
                "provider", "github"
        ), headers);
    }

    @Test
    void browse_returnsError_whenGitHubReturnsUnauthorized() {
        setupErrorDispatcher(401);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/browse", HttpMethod.POST, browseRequest(), String.class);

        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void browse_returnsError_whenGitHubRateLimited() {
        setupErrorDispatcher(429);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/browse", HttpMethod.POST, browseRequest(), String.class);

        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void activity_handlesReviewErrors_gracefully() {
        setupDispatcher(Map.of(
                "/repos/test/repo/pulls\\?.*", "pr-list-3.json",
                "/repos/test/repo/pulls/1$", "pr-single-1.json",
                "/repos/test/repo/pulls/2$", "pr-single-2.json",
                "/repos/test/repo/pulls/3$", "pr-single-3.json"
                // reviews NOT mapped → 404 → graceful fallback
        ));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("alice");
    }
}
