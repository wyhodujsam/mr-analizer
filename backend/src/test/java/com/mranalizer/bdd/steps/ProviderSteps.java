package com.mranalizer.bdd.steps;

import com.mranalizer.domain.exception.ProviderAuthException;
import com.mranalizer.domain.exception.ProviderRateLimitException;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ProviderSteps {

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    private List<MergeRequest> fetchedMrs;
    private Exception caughtException;

    @Before
    public void setUpProvider() {
        fetchedMrs = null;
        caughtException = null;
    }

    // --- Given steps ---

    @Given("a provider returning {int} merge requests for {string}")
    public void providerReturningMrs(int count, String slug) {
        List<MergeRequest> mrs = IntStream.rangeClosed(1, count)
                .mapToObj(this::buildTestMr)
                .toList();
        when(mergeRequestProvider.fetchMergeRequests(any())).thenReturn(mrs);
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
    }

    @Given("a provider returning merge requests where one has no changed files")
    public void providerReturningMrWithNoFiles() {
        MergeRequest normalMr = buildTestMr(1);

        MergeRequest emptyMr = MergeRequest.builder()
                .id(2L)
                .externalId("2")
                .title("PR #2 - empty merge")
                .description("Empty merge commit")
                .author("developer2")
                .sourceBranch("feature/empty")
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(1))
                .mergedAt(LocalDateTime.now())
                .labels(List.of())
                .changedFiles(List.of())
                .diffStats(new DiffStats(0, 0, 0))
                .hasTests(false)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(0)
                .provider("github")
                .url("https://github.com/owner/repo/pull/2")
                .projectSlug("owner/repo")
                .build();

        when(mergeRequestProvider.fetchMergeRequests(any()))
                .thenReturn(List.of(normalMr, emptyMr));
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
    }

    @Given("a provider that responds with rate limit exceeded")
    public void providerWithRateLimit() {
        when(mergeRequestProvider.fetchMergeRequests(any()))
                .thenThrow(new ProviderRateLimitException("API rate limit exceeded. Try again in 60 seconds."));
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
    }

    @Given("a provider that has no authentication token configured")
    public void providerWithNoToken() {
        when(mergeRequestProvider.fetchMergeRequests(any()))
                .thenThrow(new ProviderAuthException("GitHub token is not configured"));
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
    }

    // --- When steps ---

    @When("the system fetches merge requests for {string}")
    public void systemFetchesMrs(String slug) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .state("merged")
                .limit(100)
                .build();
        try {
            fetchedMrs = mergeRequestProvider.fetchMergeRequests(criteria);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the system fetches and filters merge requests")
    public void systemFetchesAndFiltersMrs() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(100)
                .build();
        try {
            List<MergeRequest> allMrs = mergeRequestProvider.fetchMergeRequests(criteria);
            // Filter out MRs with no changed files (domain-level filtering)
            fetchedMrs = allMrs.stream()
                    .filter(mr -> mr.getChangedFiles() != null && !mr.getChangedFiles().isEmpty())
                    .toList();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the system attempts to fetch merge requests")
    public void systemAttemptsFetch() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(100)
                .build();
        try {
            fetchedMrs = mergeRequestProvider.fetchMergeRequests(criteria);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    // --- Then steps ---

    @Then("{int} merge requests should be returned")
    public void mrsShouldBeReturned(int expectedCount) {
        assertNotNull(fetchedMrs, "Fetched merge requests should not be null");
        assertEquals(expectedCount, fetchedMrs.size());
    }

    @Then("each merge request should have a title, author, and external ID")
    public void eachMrShouldHaveFields() {
        assertNotNull(fetchedMrs);
        for (MergeRequest mr : fetchedMrs) {
            assertNotNull(mr.getTitle(), "Title should not be null");
            assertFalse(mr.getTitle().isBlank(), "Title should not be blank");
            assertNotNull(mr.getAuthor(), "Author should not be null");
            assertFalse(mr.getAuthor().isBlank(), "Author should not be blank");
            assertNotNull(mr.getExternalId(), "External ID should not be null");
            assertFalse(mr.getExternalId().isBlank(), "External ID should not be blank");
        }
    }

    @Then("the result should not contain the merge request with no changed files")
    public void resultShouldNotContainEmptyMr() {
        assertNotNull(fetchedMrs);
        for (MergeRequest mr : fetchedMrs) {
            assertFalse(mr.getChangedFiles().isEmpty(),
                    "Filtered results should not contain MR with no changed files");
        }
        assertEquals(1, fetchedMrs.size(), "Should have filtered out the empty MR");
    }

    @Then("the system should return a rate limit error with a clear message")
    public void systemShouldReturnRateLimitError() {
        assertNotNull(caughtException, "Expected an exception to be thrown");
        assertTrue(caughtException instanceof ProviderRateLimitException,
                "Expected ProviderRateLimitException but got: " + caughtException.getClass().getName());
        assertTrue(caughtException.getMessage().contains("rate limit"),
                "Error message should mention rate limit, but was: " + caughtException.getMessage());
    }

    @Then("the system should return an authentication error with message about missing token")
    public void systemShouldReturnAuthError() {
        assertNotNull(caughtException, "Expected an exception to be thrown");
        assertTrue(caughtException instanceof ProviderAuthException,
                "Expected ProviderAuthException but got: " + caughtException.getClass().getName());
        assertTrue(caughtException.getMessage().contains("token"),
                "Error message should mention token, but was: " + caughtException.getMessage());
    }

    // --- Helpers ---

    private MergeRequest buildTestMr(int index) {
        return MergeRequest.builder()
                .id((long) index)
                .externalId(String.valueOf(index))
                .title("PR #" + index + " - update service")
                .description("Update service module " + index)
                .author("developer" + index)
                .sourceBranch("feature/pr-" + index)
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(index))
                .mergedAt(LocalDateTime.now().minusDays(index).plusHours(1))
                .labels(List.of())
                .changedFiles(List.of(
                        new ChangedFile("src/main/Service" + index + ".java", 15, 5, "modified"),
                        new ChangedFile("src/test/Service" + index + "Test.java", 10, 3, "modified")
                ))
                .diffStats(new DiffStats(25, 8, 2))
                .hasTests(true)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(2)
                .provider("github")
                .url("https://github.com/owner/repo/pull/" + index)
                .projectSlug("owner/repo")
                .build();
    }
}
