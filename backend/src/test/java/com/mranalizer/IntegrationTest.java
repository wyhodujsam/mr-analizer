package com.mranalizer;

import com.mranalizer.adapter.in.rest.dto.AnalysisResponse;
import com.mranalizer.adapter.in.rest.dto.MrDetailResponse;
import com.mranalizer.application.dto.AnalysisRequestDto;
import com.mranalizer.application.dto.AnalysisSummaryDto;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private MergeRequestProvider mergeRequestProvider;

    @Test
    void postAnalysis_returnsReport() {
        setupMockProvider(3);

        AnalysisRequestDto request = new AnalysisRequestDto(
                "owner/repo", "github", "main", "merged", null, null, 100, false);

        ResponseEntity<AnalysisResponse> response = restTemplate.postForEntity(
                "/api/analysis", request, AnalysisResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalysisResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.reportId()).isNotNull();
        assertThat(body.totalMrs()).isEqualTo(3);
        assertThat(body.results()).hasSize(3);
        // Each result should have a score
        body.results().forEach(r -> assertThat(r.score()).isGreaterThanOrEqualTo(0.0));
    }

    @Test
    void getAnalysis_afterPost_returnsReport() {
        setupMockProvider(2);

        AnalysisRequestDto request = new AnalysisRequestDto(
                "owner/repo", "github", "main", "merged", null, null, 100, false);

        ResponseEntity<AnalysisResponse> postResponse = restTemplate.postForEntity(
                "/api/analysis", request, AnalysisResponse.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long reportId = postResponse.getBody().reportId();

        ResponseEntity<AnalysisResponse> getResponse = restTemplate.getForEntity(
                "/api/analysis/" + reportId, AnalysisResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalysisResponse body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.reportId()).isEqualTo(reportId);
        assertThat(body.totalMrs()).isEqualTo(2);
    }

    @Test
    void getMrDetail_returnsBreakdown() {
        setupMockProvider(1);

        AnalysisRequestDto request = new AnalysisRequestDto(
                "owner/repo", "github", "main", "merged", null, null, 100, false);

        ResponseEntity<AnalysisResponse> postResponse = restTemplate.postForEntity(
                "/api/analysis", request, AnalysisResponse.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long reportId = postResponse.getBody().reportId();
        Long resultId = postResponse.getBody().results().get(0).id();

        ResponseEntity<MrDetailResponse> detailResponse = restTemplate.getForEntity(
                "/api/analysis/" + reportId + "/mrs/" + resultId, MrDetailResponse.class);

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        MrDetailResponse detail = detailResponse.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.resultId()).isEqualTo(resultId);
        assertThat(detail.scoreBreakdown()).isNotNull();
    }

    @Test
    void getSummary_returnsCounts() {
        setupMockProvider(3);

        AnalysisRequestDto request = new AnalysisRequestDto(
                "owner/repo", "github", "main", "merged", null, null, 100, false);

        ResponseEntity<AnalysisResponse> postResponse = restTemplate.postForEntity(
                "/api/analysis", request, AnalysisResponse.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long reportId = postResponse.getBody().reportId();

        ResponseEntity<AnalysisSummaryDto> summaryResponse = restTemplate.getForEntity(
                "/api/summary/" + reportId, AnalysisSummaryDto.class);

        assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AnalysisSummaryDto summary = summaryResponse.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.totalMrs()).isEqualTo(3);
        // Total of all verdict counts should equal totalMrs
        int totalVerdicts = summary.automatable().count()
                + summary.maybe().count()
                + summary.notSuitable().count();
        assertThat(totalVerdicts).isEqualTo(3);
    }

    @Test
    void postAnalysis_invalidSlug_returns400() {
        // Empty projectSlug should trigger IllegalArgumentException from GitHubAdapter.parseOwnerRepo
        // But since provider is mocked, we need to make the mock throw
        when(mergeRequestProvider.fetchMergeRequests(any()))
                .thenThrow(new IllegalArgumentException("projectSlug must be in 'owner/repo' format, got: "));
        when(mergeRequestProvider.getProviderName()).thenReturn("github");

        AnalysisRequestDto request = new AnalysisRequestDto(
                "", "github", "main", "merged", null, null, 100, false);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/analysis", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setupMockProvider(int mrCount) {
        List<MergeRequest> mrs = java.util.stream.IntStream.rangeClosed(1, mrCount)
                .mapToObj(this::buildMr)
                .toList();

        when(mergeRequestProvider.fetchMergeRequests(any())).thenReturn(mrs);
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
    }

    private MergeRequest buildMr(int index) {
        return MergeRequest.builder()
                .id((long) index)
                .externalId(String.valueOf(index))
                .title("PR #" + index + " - refactor module")
                .description("This PR refactors module " + index + " for better readability")
                .author("developer" + index)
                .sourceBranch("feature/pr-" + index)
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(index))
                .mergedAt(LocalDateTime.now().minusDays(index).plusHours(2))
                .labels(List.of())
                .changedFiles(List.of(
                        new ChangedFile("src/main/Module" + index + ".java", 20, 5, "modified"),
                        new ChangedFile("src/test/Module" + index + "Test.java", 15, 2, "modified"),
                        new ChangedFile("src/main/Helper" + index + ".java", 10, 3, "modified"),
                        new ChangedFile("src/main/Service" + index + ".java", 8, 4, "modified"),
                        new ChangedFile("src/main/Config" + index + ".java", 5, 1, "modified")
                ))
                .diffStats(new DiffStats(58, 15, 5))
                .hasTests(true)
                .ciPassed(true)
                .approvalsCount(2)
                .commentsCount(3)
                .provider("github")
                .url("https://github.com/owner/repo/pull/" + index)
                .projectSlug("owner/repo")
                .build();
    }
}
