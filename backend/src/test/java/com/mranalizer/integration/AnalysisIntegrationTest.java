package com.mranalizer.integration;

import com.mranalizer.adapter.in.rest.dto.AnalysisResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisIntegrationTest extends GitHubIntegrationTestBase {

    @BeforeEach
    void setupRoutes() {
        setupDispatcher(Map.of(
                "/repos/test/repo/pulls\\?.*", "pr-list-3.json",
                "/repos/test/repo/pulls/1$", "pr-single-1.json",
                "/repos/test/repo/pulls/2$", "pr-single-2.json",
                "/repos/test/repo/pulls/3$", "pr-single-3.json",
                "/repos/test/repo/pulls/1/files.*", "pr-files-1.json",
                "/repos/test/repo/pulls/2/files.*", "pr-files-2.json",
                "/repos/test/repo/pulls/3/files.*", "pr-files-3.json"
        ));
    }

    private HttpEntity<Map<String, Object>> analysisRequest(List<String> mrIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "projectSlug", "test/repo",
                "provider", "github",
                "selectedMrIds", mrIds
        ), headers);
    }

    @Test
    void analysis_returnsCorrectScores_withRealisticGitHubData() {
        ResponseEntity<AnalysisResponse> response = restTemplate.exchange(
                "/api/analysis", HttpMethod.POST,
                analysisRequest(List.of("1", "2", "3")),
                AnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalysisResponse report = response.getBody();
        assertThat(report.results()).hasSize(3);
        report.results().forEach(r -> {
            assertThat(r.score()).isBetween(0.0, 1.0);
            assertThat(r.verdict()).isNotNull();
        });
    }

    @Test
    void analysis_appliesPenalty_forLargeDiff() {
        ResponseEntity<AnalysisResponse> response = restTemplate.exchange(
                "/api/analysis", HttpMethod.POST,
                analysisRequest(List.of("2")), // 800+300 = 1100 lines
                AnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = response.getBody().results().get(0);
        assertThat(result.matchedRules()).anyMatch(r ->
                r.toLowerCase().contains("large") || r.toLowerCase().contains("diff"));
    }

    @Test
    void analysis_appliesBoost_forTestFiles() {
        ResponseEntity<AnalysisResponse> response = restTemplate.exchange(
                "/api/analysis", HttpMethod.POST,
                analysisRequest(List.of("3")), // has test files
                AnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var result = response.getBody().results().get(0);
        assertThat(result.matchedRules()).anyMatch(r -> r.toLowerCase().contains("test"));
    }
}
