package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubAdapterTest {

    @Mock
    private GitHubClient client;

    @Mock
    private GitHubMapper mapper;

    private GitHubAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GitHubAdapter(client, mapper);
    }

    // --- fetchMergeRequests ---

    @Test
    void fetchMergeRequests_delegatesToClientAndMapper() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .limit(10)
                .build();

        GitHubPullRequest pr = createPr(1, "PR 1", "open",
                ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC), null);
        List<GitHubFile> files = List.of(createFile("Main.java", 10, 2, "modified"));
        MergeRequest mr = MergeRequest.builder().externalId("1").title("PR 1").build();

        when(client.fetchPullRequests("owner", "repo", "all", 10, 10)).thenReturn(List.of(pr));
        when(client.fetchFiles("owner", "repo", 1)).thenReturn(files);
        when(mapper.toDomain(pr, files, "owner/repo")).thenReturn(mr);

        List<MergeRequest> result = adapter.fetchMergeRequests(criteria);

        assertEquals(1, result.size());
        assertEquals("PR 1", result.get(0).getTitle());
        verify(client).fetchPullRequests("owner", "repo", "all", 10, 10);
        verify(client).fetchFiles("owner", "repo", 1);
        verify(mapper).toDomain(pr, files, "owner/repo");
    }

    @Test
    void fetchMergeRequests_filtersByDateRange() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .after(LocalDate.of(2026, 2, 1))
                .before(LocalDate.of(2026, 2, 28))
                .limit(100)
                .build();

        // PR created on Jan 10 - should be filtered OUT (before 'after')
        GitHubPullRequest prJan = createPr(1, "Jan PR", "open",
                ZonedDateTime.of(2026, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC), null);
        // PR created on Feb 15 - should pass
        GitHubPullRequest prFeb = createPr(2, "Feb PR", "open",
                ZonedDateTime.of(2026, 2, 15, 10, 0, 0, 0, ZoneOffset.UTC), null);
        // PR created on Mar 10 - should be filtered OUT (after 'before')
        GitHubPullRequest prMar = createPr(3, "Mar PR", "open",
                ZonedDateTime.of(2026, 3, 10, 10, 0, 0, 0, ZoneOffset.UTC), null);

        when(client.fetchPullRequests(eq("owner"), eq("repo"), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(prJan, prFeb, prMar));

        List<GitHubFile> files = Collections.emptyList();
        when(client.fetchFiles("owner", "repo", 2)).thenReturn(files);

        MergeRequest mr = MergeRequest.builder().externalId("2").title("Feb PR").build();
        when(mapper.toDomain(prFeb, files, "owner/repo")).thenReturn(mr);

        List<MergeRequest> result = adapter.fetchMergeRequests(criteria);

        assertEquals(1, result.size());
        assertEquals("Feb PR", result.get(0).getTitle());
        // Files should only be fetched for the PR that passes the filter
        verify(client, never()).fetchFiles("owner", "repo", 1);
        verify(client, never()).fetchFiles("owner", "repo", 3);
    }

    @Test
    void fetchMergeRequests_filtersByMergedState() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(100)
                .build();

        // Not merged - should be filtered out
        GitHubPullRequest prOpen = createPr(1, "Open PR", "open",
                ZonedDateTime.of(2026, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC), null);
        // Merged - should pass
        GitHubPullRequest prMerged = createPr(2, "Merged PR", "closed",
                ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                ZonedDateTime.of(2026, 1, 16, 10, 0, 0, 0, ZoneOffset.UTC));

        // state "merged" maps to API state "all"
        when(client.fetchPullRequests("owner", "repo", "all", 100, 100))
                .thenReturn(List.of(prOpen, prMerged));

        List<GitHubFile> files = Collections.emptyList();
        when(client.fetchFiles("owner", "repo", 2)).thenReturn(files);

        MergeRequest mr = MergeRequest.builder().externalId("2").title("Merged PR").build();
        when(mapper.toDomain(prMerged, files, "owner/repo")).thenReturn(mr);

        List<MergeRequest> result = adapter.fetchMergeRequests(criteria);

        assertEquals(1, result.size());
        assertEquals("Merged PR", result.get(0).getTitle());
    }

    @Test
    void fetchMergeRequests_appliesLimit() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .limit(1)
                .build();

        GitHubPullRequest pr1 = createPr(1, "PR 1", "open",
                ZonedDateTime.of(2026, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC), null);
        GitHubPullRequest pr2 = createPr(2, "PR 2", "open",
                ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC), null);

        when(client.fetchPullRequests("owner", "repo", "all", 1, 1))
                .thenReturn(List.of(pr1, pr2));

        List<GitHubFile> files = Collections.emptyList();
        when(client.fetchFiles("owner", "repo", 1)).thenReturn(files);

        MergeRequest mr = MergeRequest.builder().externalId("1").title("PR 1").build();
        when(mapper.toDomain(pr1, files, "owner/repo")).thenReturn(mr);

        List<MergeRequest> result = adapter.fetchMergeRequests(criteria);

        // Should be limited to 1 despite 2 PRs from API
        assertEquals(1, result.size());
    }

    // --- fetchMergeRequest ---

    @Test
    void fetchMergeRequest_parsesValidMrId() {
        GitHubPullRequest pr = createPr(42, "Fix", "closed",
                ZonedDateTime.now(ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC));
        List<GitHubFile> files = Collections.emptyList();
        MergeRequest mr = MergeRequest.builder().externalId("42").title("Fix").build();

        when(client.fetchPullRequest("owner", "repo", 42)).thenReturn(pr);
        when(client.fetchFiles("owner", "repo", 42)).thenReturn(files);
        when(mapper.toDomain(pr, files, "owner/repo")).thenReturn(mr);

        MergeRequest result = adapter.fetchMergeRequest("owner/repo", "42");

        assertEquals("42", result.getExternalId());
        verify(client).fetchPullRequest("owner", "repo", 42);
    }

    @Test
    void fetchMergeRequest_throwsInvalidRequestExceptionForNonNumericMrId() {
        assertThrows(InvalidRequestException.class,
                () -> adapter.fetchMergeRequest("owner/repo", "abc"));
    }

    @Test
    void fetchMergeRequest_throwsInvalidRequestExceptionForNullMrId() {
        assertThrows(InvalidRequestException.class,
                () -> adapter.fetchMergeRequest("owner/repo", null));
    }

    // --- parseOwnerRepo ---

    @Test
    void parseOwnerRepo_throwsForNullSlug() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(null)
                .limit(10)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.fetchMergeRequests(criteria));
    }

    @Test
    void parseOwnerRepo_throwsForSlugWithoutSlash() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("noslash")
                .limit(10)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.fetchMergeRequests(criteria));
    }

    @Test
    void parseOwnerRepo_throwsForSlugWithBlankParts() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("/repo")
                .limit(10)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.fetchMergeRequests(criteria));
    }

    @Test
    void parseOwnerRepo_throwsForSlugWithBlankRepo() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/")
                .limit(10)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.fetchMergeRequests(criteria));
    }

    // --- getProviderName ---

    @Test
    void getProviderName_returnsGithub() {
        assertEquals("github", adapter.getProviderName());
    }

    // --- state mapping ---

    @Test
    void fetchMergeRequests_mapsOpenStateToApi() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("open")
                .limit(10)
                .build();

        when(client.fetchPullRequests("owner", "repo", "open", 10, 10))
                .thenReturn(Collections.emptyList());

        adapter.fetchMergeRequests(criteria);

        verify(client).fetchPullRequests("owner", "repo", "open", 10, 10);
    }

    @Test
    void fetchMergeRequests_mapsClosedStateToApi() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("closed")
                .limit(10)
                .build();

        when(client.fetchPullRequests("owner", "repo", "closed", 10, 10))
                .thenReturn(Collections.emptyList());

        adapter.fetchMergeRequests(criteria);

        verify(client).fetchPullRequests("owner", "repo", "closed", 10, 10);
    }

    // --- helpers ---

    private GitHubPullRequest createPr(int number, String title, String state,
                                        ZonedDateTime createdAt, ZonedDateTime mergedAt) {
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(number);
        pr.setTitle(title);
        pr.setState(state);
        pr.setCreatedAt(createdAt);
        pr.setMergedAt(mergedAt);
        return pr;
    }

    private GitHubFile createFile(String filename, int additions, int deletions, String status) {
        GitHubFile file = new GitHubFile();
        file.setFilename(filename);
        file.setAdditions(additions);
        file.setDeletions(deletions);
        file.setStatus(status);
        return file;
    }
}
