package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "mr-analizer.provider", havingValue = "github")
public class GitHubAdapter implements MergeRequestProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubAdapter.class);

    private final GitHubClient client;
    private final GitHubMapper mapper;

    public GitHubAdapter(GitHubClient client, GitHubMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<MergeRequest> fetchMergeRequests(FetchCriteria criteria) {
        String[] parts = parseOwnerRepo(criteria.getProjectSlug());
        String owner = parts[0];
        String repo = parts[1];
        String projectSlug = criteria.getProjectSlug();

        // GitHub API: state=all to get merged PRs (merged PRs have state=closed + merged_at != null)
        String apiState = mapState(criteria.getState());
        int perPage = Math.min(criteria.getLimit(), 100);

        log.info("Fetching PRs from GitHub: {}/{} state={} limit={}", owner, repo, apiState, criteria.getLimit());

        List<GitHubPullRequest> prs = client.fetchPullRequests(owner, repo, apiState, perPage, criteria.getLimit());

        return prs.stream()
                .filter(pr -> matchesDateFilter(pr, criteria.getAfter(), criteria.getBefore()))
                .filter(pr -> matchesStateFilter(pr, criteria.getState()))
                .limit(criteria.getLimit())
                .map(pr -> {
                    List<GitHubFile> files = client.fetchFiles(owner, repo, pr.getNumber());
                    return mapper.toDomain(pr, files, projectSlug);
                })
                .collect(Collectors.toList());
    }

    @Override
    public MergeRequest fetchMergeRequest(String projectSlug, String mrId) {
        String[] parts = parseOwnerRepo(projectSlug);
        String owner = parts[0];
        String repo = parts[1];
        int number;
        try {
            number = Integer.parseInt(mrId);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid MR ID (not a number): " + mrId);
        }

        GitHubPullRequest pr = client.fetchPullRequest(owner, repo, number);
        List<GitHubFile> files = client.fetchFiles(owner, repo, number);
        return mapper.toDomain(pr, files, projectSlug);
    }

    @Override
    public String getProviderName() {
        return "github";
    }

    private String[] parseOwnerRepo(String projectSlug) {
        if (projectSlug == null || !projectSlug.contains("/")) {
            throw new IllegalArgumentException(
                    "projectSlug must be in 'owner/repo' format, got: " + projectSlug);
        }
        String[] parts = projectSlug.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "projectSlug must be in 'owner/repo' format, got: " + projectSlug);
        }
        return parts;
    }

    private String mapState(String state) {
        if (state == null) return "all";
        return switch (state.toLowerCase()) {
            case "merged" -> "all";  // GitHub has no "merged" state filter; we filter client-side
            case "open" -> "open";
            case "closed" -> "closed";
            default -> "all";
        };
    }

    private boolean matchesStateFilter(GitHubPullRequest pr, String requestedState) {
        if (requestedState == null || requestedState.isBlank()) return true;
        if ("merged".equalsIgnoreCase(requestedState)) {
            return pr.getMergedAt() != null;
        }
        return true; // already filtered by API state parameter
    }

    private boolean matchesDateFilter(GitHubPullRequest pr, LocalDate after, LocalDate before) {
        if (pr.getCreatedAt() == null) return true;
        LocalDate prDate = pr.getCreatedAt().withZoneSameInstant(ZoneOffset.UTC).toLocalDate();

        if (after != null && prDate.isBefore(after)) return false;
        if (before != null && prDate.isAfter(before)) return false;
        return true;
    }
}
