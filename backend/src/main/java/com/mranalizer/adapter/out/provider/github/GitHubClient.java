package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.exception.ProviderAuthException;
import com.mranalizer.domain.exception.ProviderException;
import com.mranalizer.domain.exception.ProviderRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final Pattern LINK_NEXT_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;

    public GitHubClient(
            @Value("${mr-analizer.github.api-url:https://api.github.com}") String apiUrl,
            @Value("${mr-analizer.github.token:}") String token) {

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.webClient = builder.build();
    }

    public List<GitHubPullRequest> fetchPullRequests(String owner, String repo, String state, int perPage, int limit) {
        List<GitHubPullRequest> allPrs = new ArrayList<>();
        String url = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls")
                .queryParam("state", state)
                .queryParam("per_page", perPage)
                .queryParam("page", 1)
                .buildAndExpand(owner, repo)
                .toUriString();

        while (url != null) {
            final String currentUrl = url;
            var response = executeWithErrorHandling(() -> webClient.get()
                    .uri(currentUrl)
                    .retrieve()
                    .toEntityList(GitHubPullRequest.class)
                    .block());

            if (response == null) {
                break;
            }

            checkRateLimit(response.getHeaders());

            List<GitHubPullRequest> body = response.getBody();
            if (body != null) {
                allPrs.addAll(body);
            }

            // Stop pagination once we have enough PRs
            if (allPrs.size() >= limit) {
                break;
            }

            url = parseNextLink(response.getHeaders());
        }

        return allPrs;
    }

    public GitHubPullRequest fetchPullRequest(String owner, String repo, int number) {
        String url = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls/{number}")
                .buildAndExpand(owner, repo, number)
                .toUriString();
        var response = executeWithErrorHandling(() -> webClient.get()
                .uri(url)
                .retrieve()
                .toEntity(GitHubPullRequest.class)
                .block());

        if (response == null || response.getBody() == null) {
            throw new ProviderException("PR #" + number + " not found in " + owner + "/" + repo);
        }
        checkRateLimit(response.getHeaders());
        return response.getBody();
    }

    public List<GitHubFile> fetchFiles(String owner, String repo, int number) {
        List<GitHubFile> allFiles = new ArrayList<>();
        String url = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls/{number}/files")
                .queryParam("per_page", 100)
                .queryParam("page", 1)
                .buildAndExpand(owner, repo, number)
                .toUriString();

        while (url != null) {
            final String currentUrl = url;
            var response = executeWithErrorHandling(() -> webClient.get()
                    .uri(currentUrl)
                    .retrieve()
                    .toEntityList(GitHubFile.class)
                    .block());

            if (response == null) {
                break;
            }

            checkRateLimit(response.getHeaders());

            List<GitHubFile> body = response.getBody();
            if (body != null) {
                allFiles.addAll(body);
            }

            url = parseNextLink(response.getHeaders());
        }

        return allFiles;
    }

    private <T> T executeWithErrorHandling(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (WebClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                throw new ProviderAuthException("GitHub API authentication failed: " + status.value());
            }
            if (status.value() == 429) {
                throw new ProviderRateLimitException("GitHub API rate limit exceeded");
            }
            if (status.value() == 404) {
                throw new ProviderException("GitHub resource not found: " + e.getMessage());
            }
            throw new ProviderException("GitHub API error " + status.value() + ": " + e.getMessage());
        }
    }

    private void checkRateLimit(HttpHeaders headers) {
        List<String> remaining = headers.get("X-RateLimit-Remaining");
        if (remaining != null && !remaining.isEmpty()) {
            try {
                int value = Integer.parseInt(remaining.get(0));
                if (value == 0) {
                    throw new ProviderRateLimitException("GitHub API rate limit exceeded");
                }
                if (value < 10) {
                    log.warn("GitHub API rate limit low: {} requests remaining", value);
                }
            } catch (NumberFormatException e) {
                // ignore unparseable header
            }
        }
    }

    private String parseNextLink(HttpHeaders headers) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return null;
        }
        for (String linkHeader : linkHeaders) {
            Matcher matcher = LINK_NEXT_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                String nextUrl = matcher.group(1);
                if (nextUrl.startsWith("http")) {
                    int idx = nextUrl.indexOf("/repos/");
                    if (idx >= 0) {
                        return nextUrl.substring(idx);
                    }
                }
                return nextUrl;
            }
        }
        return null;
    }
}
