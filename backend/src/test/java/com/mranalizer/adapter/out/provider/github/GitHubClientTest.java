package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.domain.exception.ProviderAuthException;
import com.mranalizer.domain.exception.ProviderException;
import com.mranalizer.domain.exception.ProviderRateLimitException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GitHubClientTest {

    private MockWebServer server;
    private GitHubClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString();
        // Remove trailing slash to match how WebClient handles baseUrl
        client = new GitHubClient(baseUrl, "");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // --- fetchPullRequests ---

    @Test
    void fetchPullRequests_returnsPrsFromSinglePage() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {"number": 1, "title": "PR 1", "state": "open"},
                          {"number": 2, "title": "PR 2", "state": "open"}
                        ]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 30, 100);

        assertEquals(2, prs.size());
        assertEquals(1, prs.get(0).getNumber());
        assertEquals("PR 1", prs.get(0).getTitle());
        assertEquals(2, prs.get(1).getNumber());
    }

    @Test
    void fetchPullRequests_followsPagination() {
        String page2Url = server.url("/repos/owner/repo/pulls?state=open&per_page=2&page=2").toString();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", "<" + page2Url + ">; rel=\"next\"")
                .setBody("""
                        [{"number": 1, "title": "PR 1", "state": "open"}]
                        """));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"number": 2, "title": "PR 2", "state": "open"}]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 2, 100);

        assertEquals(2, prs.size());
        assertEquals(1, prs.get(0).getNumber());
        assertEquals(2, prs.get(1).getNumber());
    }

    @Test
    void fetchPullRequests_stopsAfterReachingLimit() {
        String page2Url = server.url("/repos/owner/repo/pulls?state=open&per_page=2&page=2").toString();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", "<" + page2Url + ">; rel=\"next\"")
                .setBody("""
                        [
                          {"number": 1, "title": "PR 1", "state": "open"},
                          {"number": 2, "title": "PR 2", "state": "open"}
                        ]
                        """));
        // This second page should NOT be fetched because limit=2 and we already have 2
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"number": 3, "title": "PR 3", "state": "open"}]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 2, 2);

        assertEquals(2, prs.size());
        // Only 1 request should have been made
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void fetchPullRequests_handlesEmptyResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        var prs = client.fetchPullRequests("owner", "repo", "open", 30, 100);

        assertTrue(prs.isEmpty());
    }

    // --- fetchPullRequest ---

    @Test
    void fetchPullRequest_returnsSinglePr() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "number": 42,
                          "title": "Fix bug",
                          "body": "Fixes issue #1",
                          "state": "closed",
                          "merged_at": "2026-01-15T10:30:00Z",
                          "created_at": "2026-01-10T08:00:00Z",
                          "html_url": "https://github.com/owner/repo/pull/42",
                          "user": {"login": "developer"},
                          "head": {"ref": "feature-branch"},
                          "base": {"ref": "main"},
                          "labels": [{"name": "bugfix"}]
                        }
                        """));

        var pr = client.fetchPullRequest("owner", "repo", 42);

        assertEquals(42, pr.getNumber());
        assertEquals("Fix bug", pr.getTitle());
        assertEquals("Fixes issue #1", pr.getBody());
        assertEquals("closed", pr.getState());
        assertNotNull(pr.getMergedAt());
        assertNotNull(pr.getCreatedAt());
        assertEquals("https://github.com/owner/repo/pull/42", pr.getHtmlUrl());
        assertEquals("developer", pr.getUser().getLogin());
        assertEquals("feature-branch", pr.getHead().getRef());
        assertEquals("main", pr.getBase().getRef());
        assertEquals(1, pr.getLabels().size());
        assertEquals("bugfix", pr.getLabels().get(0).getName());
    }

    @Test
    void fetchPullRequest_throwsProviderExceptionWhenNotFound() {
        // Simulate null body (empty 200 response that deserializes to null)
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(404)
                .setBody("{\"message\": \"Not Found\"}"));

        assertThrows(ProviderException.class,
                () -> client.fetchPullRequest("owner", "repo", 999));
    }

    // --- fetchFiles ---

    @Test
    void fetchFiles_returnsFilesWithPagination() {
        String page2Url = server.url("/repos/owner/repo/pulls/1/files?per_page=100&page=2").toString();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", "<" + page2Url + ">; rel=\"next\"")
                .setBody("""
                        [{"filename": "src/Main.java", "additions": 10, "deletions": 2, "status": "modified"}]
                        """));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"filename": "src/Test.java", "additions": 5, "deletions": 0, "status": "added"}]
                        """));

        var files = client.fetchFiles("owner", "repo", 1);

        assertEquals(2, files.size());
        assertEquals("src/Main.java", files.get(0).getFilename());
        assertEquals(10, files.get(0).getAdditions());
        assertEquals(2, files.get(0).getDeletions());
        assertEquals("modified", files.get(0).getStatus());
        assertEquals("src/Test.java", files.get(1).getFilename());
    }

    // --- HTTP error handling ---

    @Test
    void fetchPullRequests_http401_throwsProviderAuthException() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        assertThrows(ProviderAuthException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
    }

    @Test
    void fetchPullRequests_http403_throwsProviderAuthException() {
        server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));

        assertThrows(ProviderAuthException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
    }

    @Test
    void fetchPullRequests_http429_throwsProviderRateLimitException() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("Too Many Requests"));

        assertThrows(ProviderRateLimitException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
    }

    @Test
    void fetchPullRequests_http404_throwsProviderException() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        assertThrows(ProviderException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
    }

    // --- Rate limit header checks ---

    @Test
    void fetchPullRequests_rateLimitRemainingZero_throwsProviderRateLimitException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "0")
                .setBody("""
                        [{"number": 1, "title": "PR 1", "state": "open"}]
                        """));

        assertThrows(ProviderRateLimitException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
    }

    @Test
    void fetchPullRequests_rateLimitRemainingLow_logsWarningButSucceeds() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "5")
                .setBody("""
                        [{"number": 1, "title": "PR 1", "state": "open"}]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 30, 100);

        // Should still return results despite low rate limit
        assertEquals(1, prs.size());
    }

    @Test
    void fetchPullRequest_rateLimitRemainingZero_throwsProviderRateLimitException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "0")
                .setBody("""
                        {"number": 1, "title": "PR 1", "state": "open"}
                        """));

        assertThrows(ProviderRateLimitException.class,
                () -> client.fetchPullRequest("owner", "repo", 1));
    }

    @Test
    void fetchPullRequests_http500_throwsProviderException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        ProviderException ex = assertThrows(ProviderException.class,
                () -> client.fetchPullRequests("owner", "repo", "open", 30, 100));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void fetchPullRequests_rateLimitUnparseableHeader_succeedsWithoutException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "not-a-number")
                .setBody("""
                        [{"number": 1, "title": "PR 1", "state": "open"}]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 30, 100);
        assertEquals(1, prs.size());
    }

    @Test
    void fetchPullRequests_linkHeaderWithRelativePath_followsPagination() {
        // Link header with a path that doesn't start with http and doesn't contain /repos/
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", "</repos/owner/repo/pulls?page=2>; rel=\"next\"")
                .setBody("""
                        [{"number": 1, "title": "PR 1", "state": "open"}]
                        """));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"number": 2, "title": "PR 2", "state": "open"}]
                        """));

        var prs = client.fetchPullRequests("owner", "repo", "open", 30, 100);
        assertEquals(2, prs.size());
    }

    @Test
    void fetchFiles_http401_throwsProviderAuthException() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        assertThrows(ProviderAuthException.class,
                () -> client.fetchFiles("owner", "repo", 1));
    }
}
