package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.adapter.out.provider.github.dto.GitHubReview;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final Pattern LINK_NEXT_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final WebClient webClient;
    private final boolean tokenConfigured;

    public GitHubClient(
            @Value("${mr-analizer.github.api-url:https://api.github.com}") String apiUrl,
            @Value("${mr-analizer.github.token:}") String token) {

        this.tokenConfigured = token != null && !token.isBlank();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");

        if (tokenConfigured) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.webClient = builder.build();
    }

    private void requireToken() {
        if (!tokenConfigured) {
            throw new ProviderAuthException("GitHub token is not configured. Set GITHUB_TOKEN environment variable.");
        }
    }

    public List<GitHubPullRequest> fetchPullRequests(String owner, String repo, String state, int perPage, int limit) {
        requireToken();
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

    private static final int MAX_INCREMENTAL_PAGES = 10;

    public List<GitHubPullRequest> fetchPullRequestsUpdatedSince(
            String owner, String repo, LocalDateTime since) {
        requireToken();
        String sinceStr = since.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        List<GitHubPullRequest> allPrs = new ArrayList<>();
        int pages = 0;
        String url = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls")
                .queryParam("state", "all")
                .queryParam("sort", "updated")
                .queryParam("direction", "desc")
                .queryParam("per_page", 100)
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

            if (response == null) break;

            checkRateLimit(response.getHeaders());

            List<GitHubPullRequest> body = response.getBody();
            if (body == null || body.isEmpty()) break;

            boolean reachedOlder = false;
            for (GitHubPullRequest pr : body) {
                if (pr.getUpdatedAt() != null && pr.getUpdatedAt()
                        .toInstant().isBefore(since.toInstant(ZoneOffset.UTC))) {
                    reachedOlder = true;
                    break;
                }
                allPrs.add(pr);
            }

            if (reachedOlder) break;

            pages++;
            if (pages >= MAX_INCREMENTAL_PAGES) {
                log.warn("Incremental fetch for {}/{} hit page limit ({}), stopping",
                        owner, repo, MAX_INCREMENTAL_PAGES);
                break;
            }

            url = parseNextLink(response.getHeaders());
        }

        log.info("Incremental fetch for {}/{}: {} PRs updated since {}",
                owner, repo, allPrs.size(), sinceStr);
        return allPrs;
    }

    public GitHubPullRequest fetchPullRequest(String owner, String repo, int number) {
        requireToken();
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
        requireToken();
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

    public List<GitHubReview> fetchReviews(String owner, String repo, int number) {
        requireToken();
        String url = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls/{number}/reviews")
                .queryParam("per_page", 100)
                .buildAndExpand(owner, repo, number)
                .toUriString();

        var response = executeWithErrorHandling(() -> webClient.get()
                .uri(url)
                .retrieve()
                .toEntityList(GitHubReview.class)
                .block());

        if (response == null || response.getBody() == null) {
            return List.of();
        }
        checkRateLimit(response.getHeaders());
        return response.getBody();
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
