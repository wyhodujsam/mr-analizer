package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.exception.ProviderRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    public List<GitHubPullRequest> fetchPullRequests(String owner, String repo, String state, int perPage) {
        List<GitHubPullRequest> allPrs = new ArrayList<>();
        String url = String.format("/repos/%s/%s/pulls?state=%s&per_page=%d&page=1", owner, repo, state, perPage);

        while (url != null) {
            var response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntityList(GitHubPullRequest.class)
                    .block();

            if (response == null) {
                break;
            }

            checkRateLimit(response.getHeaders());

            List<GitHubPullRequest> body = response.getBody();
            if (body != null) {
                allPrs.addAll(body);
            }

            url = parseNextLink(response.getHeaders());
        }

        return allPrs;
    }

    public List<GitHubFile> fetchFiles(String owner, String repo, int number) {
        List<GitHubFile> allFiles = new ArrayList<>();
        String url = String.format("/repos/%s/%s/pulls/%d/files?per_page=100&page=1", owner, repo, number);

        while (url != null) {
            var response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntityList(GitHubFile.class)
                    .block();

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
                // If it's a full URL, extract the path+query portion
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
