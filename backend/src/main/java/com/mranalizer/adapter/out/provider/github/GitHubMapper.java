package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GitHubMapper {

    private static final List<String> TEST_INDICATORS = List.of("test", "Test", "spec", "Spec", "__tests__");

    public MergeRequest toDomain(GitHubPullRequest pr, List<GitHubFile> files, String projectSlug) {
        List<ChangedFile> changedFiles = files.stream()
                .map(f -> new ChangedFile(f.getFilename(), f.getAdditions(), f.getDeletions(), f.getStatus()))
                .collect(Collectors.toList());

        int totalAdditions = files.stream().mapToInt(GitHubFile::getAdditions).sum();
        int totalDeletions = files.stream().mapToInt(GitHubFile::getDeletions).sum();
        DiffStats diffStats = new DiffStats(totalAdditions, totalDeletions, files.size());

        boolean hasTests = files.stream()
                .anyMatch(f -> TEST_INDICATORS.stream().anyMatch(ind -> f.getFilename().contains(ind)));

        List<String> labels = pr.getLabels() != null
                ? pr.getLabels().stream()
                    .map(GitHubPullRequest.Label::getName)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        // Map GitHub state: if merged_at is present, state is "merged"
        String state = pr.getMergedAt() != null ? "merged" : pr.getState();

        return MergeRequest.builder()
                .externalId(String.valueOf(pr.getNumber()))
                .title(pr.getTitle())
                .description(pr.getBody())
                .author(pr.getUser() != null ? pr.getUser().getLogin() : "unknown")
                .sourceBranch(pr.getHead() != null ? pr.getHead().getRef() : null)
                .targetBranch(pr.getBase() != null ? pr.getBase().getRef() : null)
                .state(state)
                .createdAt(toLocalDateTime(pr.getCreatedAt()))
                .mergedAt(toLocalDateTime(pr.getMergedAt()))
                .labels(labels)
                .changedFiles(changedFiles)
                .diffStats(diffStats)
                .hasTests(hasTests)
                .provider("github")
                .url(pr.getHtmlUrl())
                .projectSlug(projectSlug)
                .build();
    }

    /**
     * Map PR without fetching files — for browse (list) operations.
     * DiffStats come from PR metadata fields (additions, deletions, changed_files).
     */
    public MergeRequest toDomainWithoutFiles(GitHubPullRequest pr, String projectSlug) {
        DiffStats diffStats = new DiffStats(pr.getAdditions(), pr.getDeletions(), pr.getChangedFilesCount());

        List<String> labels = pr.getLabels() != null
                ? pr.getLabels().stream()
                    .map(GitHubPullRequest.Label::getName)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        String state = pr.getMergedAt() != null ? "merged" : pr.getState();

        return MergeRequest.builder()
                .externalId(String.valueOf(pr.getNumber()))
                .title(pr.getTitle())
                .description(pr.getBody())
                .author(pr.getUser() != null ? pr.getUser().getLogin() : "unknown")
                .sourceBranch(pr.getHead() != null ? pr.getHead().getRef() : null)
                .targetBranch(pr.getBase() != null ? pr.getBase().getRef() : null)
                .state(state)
                .createdAt(toLocalDateTime(pr.getCreatedAt()))
                .mergedAt(toLocalDateTime(pr.getMergedAt()))
                .labels(labels)
                .changedFiles(List.of())
                .diffStats(diffStats)
                .hasTests(false)
                .provider("github")
                .url(pr.getHtmlUrl())
                .projectSlug(projectSlug)
                .build();
    }

    private LocalDateTime toLocalDateTime(ZonedDateTime zdt) {
        if (zdt == null) return null;
        return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
