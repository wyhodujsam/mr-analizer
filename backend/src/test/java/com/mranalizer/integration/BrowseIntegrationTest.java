package com.mranalizer.integration;

import com.mranalizer.adapter.in.rest.dto.MrBrowseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BrowseIntegrationTest extends GitHubIntegrationTestBase {

    @BeforeEach
    void setupRoutes() {
        setupDispatcher(Map.of(
                "/repos/test/repo/pulls\\?.*", "pr-list-3.json"
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
    void browse_returnsZeroChangedFiles_whenGitHubListDoesNotIncludeThem() {
        ResponseEntity<List<MrBrowseResponse>> response = restTemplate.exchange(
                "/api/browse", HttpMethod.POST, browseRequest(),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        response.getBody().forEach(pr ->
                assertThat(pr.changedFilesCount())
                        .as("PR #%s changedFilesCount from list endpoint", pr.externalId())
                        .isZero());
    }

    @Test
    void browse_returnsCorrectMetadata() {
        ResponseEntity<List<MrBrowseResponse>> response = restTemplate.exchange(
                "/api/browse", HttpMethod.POST, browseRequest(),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MrBrowseResponse pr1 = response.getBody().stream()
                .filter(p -> "1".equals(p.externalId())).findFirst().orElseThrow();
        assertThat(pr1.title()).isEqualTo("Add user authentication");
        assertThat(pr1.author()).isEqualTo("alice");
        assertThat(pr1.state()).isEqualTo("merged");
    }
}
