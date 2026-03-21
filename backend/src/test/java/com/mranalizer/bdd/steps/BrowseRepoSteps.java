package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.application.BrowseMrService;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BrowseRepoSteps {

    @Autowired
    private BrowseMrService browseMrService;

    @Autowired
    private ManageReposUseCase manageReposUseCase;

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    private List<MergeRequest> browseResult;
    private List<SavedRepository> savedRepos;

    @Before
    public void setUp() {
        browseResult = null;
        savedRepos = null;
        browseMrService.invalidateCache("owner/repo");
        browseMrService.invalidateCache("other/repo");
    }

    // --- When steps ---

    @When("I browse merge requests for {string}")
    public void browseMergeRequestsFor(String slug) {
        when(mergeRequestProvider.getProviderName()).thenReturn("github");

        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .targetBranch("main")
                .state("merged")
                .limit(100)
                .build();
        browseResult = browseMrService.browse(criteria);
    }

    @When("I browse merge requests for {string} again")
    public void browseMergeRequestsForAgain(String slug) {
        browseMergeRequestsFor(slug);
    }

    @When("I retrieve all saved repositories")
    public void retrieveAllSavedRepositories() {
        savedRepos = manageReposUseCase.getAll();
    }

    @When("I delete the saved repository {string}")
    public void deleteTheSavedRepository(String slug) {
        Optional<SavedRepository> repo = manageReposUseCase.findBySlug(slug);
        assertTrue(repo.isPresent(), "Expected saved repository for slug: " + slug);
        manageReposUseCase.delete(repo.get().getId());
    }

    // --- Then steps ---

    @Then("the browse result should contain {int} merge requests")
    public void browseResultShouldContain(int expectedCount) {
        assertNotNull(browseResult, "Browse result should not be null");
        assertEquals(expectedCount, browseResult.size());
    }

    @Then("no merge request should have a score")
    public void noMergeRequestShouldHaveScore() {
        // Browse returns raw MergeRequest objects without scoring — they have no score field.
        // This step verifies the browse result is a list of MergeRequest (not AnalysisResult).
        assertNotNull(browseResult);
        assertFalse(browseResult.isEmpty());
        // MergeRequest has no getScore() method — this confirms no scoring was done.
        for (MergeRequest mr : browseResult) {
            assertNotNull(mr.getTitle(), "Each MR should have a title");
            assertNotNull(mr.getExternalId(), "Each MR should have an external ID");
        }
    }

    @Then("the saved repos list should contain {string}")
    public void savedReposListShouldContain(String slug) {
        List<SavedRepository> repos = manageReposUseCase.getAll();
        assertTrue(repos.stream().anyMatch(r -> slug.equals(r.getProjectSlug())),
                "Expected saved repos to contain '" + slug + "' but found: "
                        + repos.stream().map(SavedRepository::getProjectSlug).toList());
    }

    @When("I force refresh browse for {string}")
    public void forceRefreshBrowseFor(String slug) {
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .targetBranch("main")
                .state("merged")
                .limit(100)
                .build();
        browseResult = browseMrService.browse(criteria, true);
    }

    @Then("the provider should have been called only once for browse")
    public void providerCalledOnceForBrowse() {
        verify(mergeRequestProvider, times(1)).fetchMergeRequests(any());
    }

    @Then("the provider should have been called twice for browse")
    public void providerCalledTwiceForBrowse() {
        verify(mergeRequestProvider, times(2)).fetchMergeRequests(any());
    }

    @Then("the saved repos list should not contain {string}")
    public void savedReposListShouldNotContain(String slug) {
        List<SavedRepository> repos = manageReposUseCase.getAll();
        assertTrue(repos.stream().noneMatch(r -> slug.equals(r.getProjectSlug())),
                "Expected saved repos NOT to contain '" + slug + "'");
    }
}
