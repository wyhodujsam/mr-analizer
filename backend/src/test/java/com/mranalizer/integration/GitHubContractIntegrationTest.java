package com.mranalizer.integration;

import com.mranalizer.adapter.in.rest.dto.MrBrowseResponse;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubContractIntegrationTest extends GitHubIntegrationTestBase {

    @Autowired
    private ActivityAnalysisUseCase activityAnalysis;

    @BeforeEach
    void setupRoutes() {
        activityAnalysis.invalidateCache("test/repo");
        setupDispatcher(Map.of(
                "/repos/test/repo/pulls\\?.*", "pr-list-3.json",
                "/repos/test/repo/pulls/1$", "pr-single-1.json",
                "/repos/test/repo/pulls/1/files.*", "pr-files-1.json",
                "/repos/test/repo/pulls/1/reviews.*", "pr-reviews-approved.json",
                "/repos/test/repo/pulls/2$", "pr-single-2.json",
                "/repos/test/repo/pulls/3$", "pr-single-3.json"
        ));
    }

    private HttpEntity<Map<String, Object>> browseRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "projectSlug", "test/repo",
                "provider", "github"
        ), headers);
    }

    @Test
    void fullPrJson_mappedCorrectly_toRestResponse() {
        ResponseEntity<List<MrBrowseResponse>> response = restTemplate.exchange(
                "/api/browse", HttpMethod.POST, browseRequest(),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MrBrowseResponse pr1 = response.getBody().stream()
                .filter(p -> "1".equals(p.externalId())).findFirst().orElseThrow();

        assertThat(pr1.title()).isEqualTo("Add user authentication");
        assertThat(pr1.author()).isEqualTo("alice");
        assertThat(pr1.state()).isEqualTo("merged");
        assertThat(pr1.createdAt()).contains("2026-03-10");
        assertThat(pr1.mergedAt()).contains("2026-03-10");
        assertThat(pr1.labels()).containsExactly("feature");
    }

    @Test
    void reviewsJson_mappedCorrectly_toActivityFlags() {
        // PR #1 has external APPROVED review from "carol" → should NOT flag NO_REVIEW
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/activity/test/repo/report?author=alice", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // PR #1 with external review should not have NO_REVIEW flag
        assertThat(response.getBody()).doesNotContain("\"prReference\":\"#1\"");
    }
}
