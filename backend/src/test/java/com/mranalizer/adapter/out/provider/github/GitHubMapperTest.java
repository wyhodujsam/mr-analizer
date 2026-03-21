package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubFile;
import com.mranalizer.adapter.out.provider.github.dto.GitHubPullRequest;
import com.mranalizer.domain.model.MergeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHubMapperTest {

    private GitHubMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GitHubMapper();
    }

    @Test
    void toDomain_mapsAllFieldsCorrectly() {
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(42);
        pr.setTitle("Add feature X");
        pr.setBody("This PR adds feature X");
        pr.setState("closed");
        pr.setCreatedAt(ZonedDateTime.of(2026, 1, 10, 8, 0, 0, 0, ZoneOffset.UTC));
        pr.setMergedAt(ZonedDateTime.of(2026, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC));
        pr.setHtmlUrl("https://github.com/owner/repo/pull/42");

        GitHubPullRequest.User user = new GitHubPullRequest.User();
        user.setLogin("developer");
        pr.setUser(user);

        GitHubPullRequest.Ref head = new GitHubPullRequest.Ref();
        head.setRef("feature-x");
        pr.setHead(head);

        GitHubPullRequest.Ref base = new GitHubPullRequest.Ref();
        base.setRef("main");
        pr.setBase(base);

        GitHubPullRequest.Label label = new GitHubPullRequest.Label();
        label.setName("enhancement");
        pr.setLabels(List.of(label));

        GitHubFile file1 = createFile("src/Main.java", 10, 2, "modified");
        GitHubFile file2 = createFile("src/Helper.java", 5, 3, "added");

        MergeRequest result = mapper.toDomain(pr, List.of(file1, file2), "owner/repo");

        assertEquals("42", result.getExternalId());
        assertEquals("Add feature X", result.getTitle());
        assertEquals("This PR adds feature X", result.getDescription());
        assertEquals("developer", result.getAuthor());
        assertEquals("feature-x", result.getSourceBranch());
        assertEquals("main", result.getTargetBranch());
        assertEquals("merged", result.getState()); // merged_at != null => "merged"
        assertEquals(LocalDateTime.of(2026, 1, 10, 8, 0, 0), result.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 15, 10, 30, 0), result.getMergedAt());
        assertEquals(List.of("enhancement"), result.getLabels());
        assertEquals("https://github.com/owner/repo/pull/42", result.getUrl());
        assertEquals("github", result.getProvider());
        assertEquals("owner/repo", result.getProjectSlug());

        // Changed files
        assertEquals(2, result.getChangedFiles().size());
        assertEquals("src/Main.java", result.getChangedFiles().get(0).path());
        assertEquals(10, result.getChangedFiles().get(0).additions());
        assertEquals(2, result.getChangedFiles().get(0).deletions());
        assertEquals("modified", result.getChangedFiles().get(0).status());

        // DiffStats
        assertEquals(15, result.getDiffStats().additions()); // 10 + 5
        assertEquals(5, result.getDiffStats().deletions());   // 2 + 3
        assertEquals(2, result.getDiffStats().changedFilesCount());

        // No test files
        assertFalse(result.hasTests());
    }

    @Test
    void toDomain_handlesNullFields() {
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(1);
        pr.setTitle("Minimal PR");
        pr.setState("open");
        // user, head, base, labels, mergedAt, createdAt, body, htmlUrl all null

        MergeRequest result = mapper.toDomain(pr, Collections.emptyList(), "owner/repo");

        assertEquals("1", result.getExternalId());
        assertEquals("Minimal PR", result.getTitle());
        assertNull(result.getDescription());
        assertEquals("unknown", result.getAuthor()); // null user => "unknown"
        assertNull(result.getSourceBranch()); // null head
        assertNull(result.getTargetBranch()); // null base
        assertEquals("open", result.getState()); // no merged_at => uses state field
        assertNull(result.getCreatedAt());
        assertNull(result.getMergedAt());
        assertTrue(result.getLabels().isEmpty()); // null labels => empty
        assertTrue(result.getChangedFiles().isEmpty());
        assertEquals(0, result.getDiffStats().additions());
        assertEquals(0, result.getDiffStats().deletions());
        assertEquals(0, result.getDiffStats().changedFilesCount());
        assertFalse(result.hasTests());
    }

    @Test
    void toDomain_detectsTestFiles_withTestInPath() {
        GitHubPullRequest pr = minimalPr();
        GitHubFile testFile = createFile("src/test/java/MyTest.java", 20, 0, "added");

        MergeRequest result = mapper.toDomain(pr, List.of(testFile), "owner/repo");

        assertTrue(result.hasTests());
    }

    @Test
    void toDomain_detectsTestFiles_withSpecInPath() {
        GitHubPullRequest pr = minimalPr();
        GitHubFile specFile = createFile("spec/MySpec.js", 15, 0, "added");

        MergeRequest result = mapper.toDomain(pr, List.of(specFile), "owner/repo");

        assertTrue(result.hasTests());
    }

    @Test
    void toDomain_detectsTestFiles_withTestsDirectory() {
        GitHubPullRequest pr = minimalPr();
        GitHubFile testFile = createFile("__tests__/component.test.js", 10, 0, "added");

        MergeRequest result = mapper.toDomain(pr, List.of(testFile), "owner/repo");

        assertTrue(result.hasTests());
    }

    @Test
    void toDomain_noTestFiles_returnsFalse() {
        GitHubPullRequest pr = minimalPr();
        GitHubFile srcFile = createFile("src/main/java/Service.java", 30, 5, "modified");

        MergeRequest result = mapper.toDomain(pr, List.of(srcFile), "owner/repo");

        assertFalse(result.hasTests());
    }

    @Test
    void toDomain_computesDiffStatsFromFiles() {
        GitHubPullRequest pr = minimalPr();
        GitHubFile file1 = createFile("a.java", 100, 20, "modified");
        GitHubFile file2 = createFile("b.java", 50, 10, "modified");
        GitHubFile file3 = createFile("c.java", 0, 30, "deleted");

        MergeRequest result = mapper.toDomain(pr, List.of(file1, file2, file3), "owner/repo");

        assertEquals(150, result.getDiffStats().additions());
        assertEquals(60, result.getDiffStats().deletions());
        assertEquals(3, result.getDiffStats().changedFilesCount());
    }

    @Test
    void toDomain_stateIsMerged_whenMergedAtPresent() {
        GitHubPullRequest pr = minimalPr();
        pr.setState("closed");
        pr.setMergedAt(ZonedDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));

        MergeRequest result = mapper.toDomain(pr, Collections.emptyList(), "owner/repo");

        assertEquals("merged", result.getState());
    }

    @Test
    void toDomain_stateIsOriginal_whenMergedAtNull() {
        GitHubPullRequest pr = minimalPr();
        pr.setState("open");
        pr.setMergedAt(null);

        MergeRequest result = mapper.toDomain(pr, Collections.emptyList(), "owner/repo");

        assertEquals("open", result.getState());
    }

    // --- helpers ---

    private GitHubPullRequest minimalPr() {
        GitHubPullRequest pr = new GitHubPullRequest();
        pr.setNumber(1);
        pr.setTitle("Test PR");
        pr.setState("open");
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
