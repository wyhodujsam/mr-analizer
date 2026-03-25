package com.mranalizer.integration;

import com.mranalizer.adapter.in.rest.project.dto.ProjectAnalysisResponse;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import com.mranalizer.domain.port.in.project.ProjectAnalysisUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectAnalysisIntegrationTest extends GitHubIntegrationTestBase {

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
                Map.entry("/repos/test/repo/pulls/1/files.*", "pr-files-bdd.json"),
                Map.entry("/repos/test/repo/pulls/2/files.*", "pr-files-sdd.json"),
                Map.entry("/repos/test/repo/pulls/3/files.*", "pr-files-3.json"),
                Map.entry("/repos/test/repo/pulls/1/reviews.*", "pr-reviews-approved.json"),
                Map.entry("/repos/test/repo/pulls/2/reviews.*", "pr-reviews-empty.json"),
                Map.entry("/repos/test/repo/pulls/3/reviews.*", "pr-reviews-approved.json")
        ));
    }

    @Test
    void analyzeProject_returnsFullResult() {
        ResponseEntity<ProjectAnalysisResponse> response = restTemplate.postForEntity(
                "/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProjectAnalysisResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.summary().totalPrs()).isEqualTo(3);
        assertThat(body.rows()).hasSize(3);
        assertThat(body.summary().automatableCount() + body.summary().maybeCount()
                + body.summary().notSuitableCount()).isEqualTo(3);
        assertThat(body.summary().histogram()).hasSize(5);
    }

    @Test
    void analyzeProject_detectsBddAndSdd() {
        ResponseEntity<ProjectAnalysisResponse> response = restTemplate.postForEntity(
                "/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        var rows = response.getBody().rows();
        // PR 1 has BDD files (auth.feature, AuthSteps.java)
        var pr1 = rows.stream().filter(r -> r.prId().equals("1")).findFirst().orElseThrow();
        assertThat(pr1.hasBdd()).isTrue();
        assertThat(pr1.bddFiles()).isNotEmpty();

        // PR 2 has SDD files (spec.md, plan.md)
        var pr2 = rows.stream().filter(r -> r.prId().equals("2")).findFirst().orElseThrow();
        assertThat(pr2.hasSdd()).isTrue();
        assertThat(pr2.sddFiles()).isNotEmpty();

        assertThat(response.getBody().summary().bddCount()).isGreaterThanOrEqualTo(1);
        assertThat(response.getBody().summary().sddCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void analyzeProject_includesRuleResultsForDrillDown() {
        ResponseEntity<ProjectAnalysisResponse> response = restTemplate.postForEntity(
                "/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        // At least some PRs should have matched rules
        var allRules = response.getBody().rows().stream()
                .flatMap(r -> r.ruleResults().stream())
                .toList();
        assertThat(allRules).isNotEmpty();
        allRules.forEach(rr -> {
            assertThat(rr.ruleName()).isNotBlank();
            assertThat(rr.reason()).isNotBlank();
        });
    }

    @Test
    void getSavedAnalyses_afterAnalyze_returnsList() {
        // First analyze to create a saved result
        restTemplate.postForEntity("/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        ResponseEntity<ProjectAnalysisResponse[]> response = restTemplate.getForEntity(
                "/api/project/test/repo/analyses", ProjectAnalysisResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getSavedAnalysisById_returnsResult() {
        ResponseEntity<ProjectAnalysisResponse> analyzeResponse = restTemplate.postForEntity(
                "/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        Long id = analyzeResponse.getBody().id();
        assertThat(id).isNotNull();

        ResponseEntity<ProjectAnalysisResponse> getResponse = restTemplate.getForEntity(
                "/api/project/analyses/" + id, ProjectAnalysisResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().summary().totalPrs()).isEqualTo(3);
    }

    @Test
    void deleteAnalysis_returns204() {
        ResponseEntity<ProjectAnalysisResponse> analyzeResponse = restTemplate.postForEntity(
                "/api/project/test/repo/analyze?useLlm=false",
                null, ProjectAnalysisResponse.class);

        Long id = analyzeResponse.getBody().id();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/project/analyses/" + id, HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getAnalysisById_notFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/project/analyses/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
