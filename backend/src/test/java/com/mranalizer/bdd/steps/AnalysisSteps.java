package com.mranalizer.bdd.steps;

import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnalysisSteps {

    @Autowired
    private AnalyzeMrUseCase analyzeMrUseCase;

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    @Autowired
    private LlmAnalyzer llmAnalyzer;

    @Autowired
    private ScenarioContext scenarioContext;

    private AnalysisReport report;
    private Exception caughtException;

    @Before
    public void setUp() {
        report = null;
        caughtException = null;

        // Reset mocks before each scenario
        reset(mergeRequestProvider, llmAnalyzer);

        // Default mock behavior
        when(mergeRequestProvider.getProviderName()).thenReturn("github");
        when(llmAnalyzer.getProviderName()).thenReturn("none");
        when(llmAnalyzer.analyze(any())).thenReturn(new LlmAssessment(0.0, null, "none"));
    }

    // --- Given steps ---

    @Given("a repository {string} with merge requests")
    public void repositoryWithMergeRequests(String slug) {
        // Background step — sets up the context; actual MR data configured in later steps
    }

    @Given("the repository has {int} merge request(s)")
    public void repositoryHasMergeRequests(int count) {
        List<MergeRequest> mrs = IntStream.rangeClosed(1, count)
                .mapToObj(this::buildTestMr)
                .toList();
        when(mergeRequestProvider.fetchMergeRequests(any())).thenReturn(mrs);
    }

    @Given("the provider will fail with {string}")
    public void providerWillFail(String errorMessage) {
        when(mergeRequestProvider.fetchMergeRequests(any()))
                .thenThrow(new IllegalArgumentException(errorMessage));
    }

    @Given("the LLM analyzer returns a comment {string}")
    public void llmAnalyzerReturnsComment(String comment) {
        when(llmAnalyzer.analyze(any()))
                .thenReturn(new LlmAssessment(0.05, comment, "claude-cli"));
        when(llmAnalyzer.getProviderName()).thenReturn("claude-cli");
    }

    @Given("the LLM analyzer times out")
    public void llmAnalyzerTimesOut() {
        when(llmAnalyzer.analyze(any()))
                .thenThrow(new RuntimeException("LLM timeout", new TimeoutException("Connection timed out")));
        when(llmAnalyzer.getProviderName()).thenReturn("claude-cli");
    }

    // --- When steps ---

    @When("I trigger analysis for {string}")
    public void triggerAnalysis(String slug) {
        triggerAnalysisInternal(slug, false);
    }

    @When("I trigger analysis for {string} with LLM enabled")
    public void triggerAnalysisWithLlm(String slug) {
        triggerAnalysisInternal(slug, true);
    }

    @When("I trigger analysis for {string} with LLM disabled")
    public void triggerAnalysisWithoutLlm(String slug) {
        triggerAnalysisInternal(slug, false);
    }

    private void triggerAnalysisInternal(String slug, boolean withLlm) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .targetBranch("main")
                .state("merged")
                .limit(100)
                .build();
        try {
            report = analyzeMrUseCase.analyze(criteria, withLlm, List.of());
            scenarioContext.setLastAnalysisReport(report);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    // --- Then steps ---

    @Then("the analysis report should contain {int} results")
    public void reportShouldContainResults(int expectedCount) {
        assertNotNull(report, "Analysis report should not be null");
        assertEquals(expectedCount, report.getResults().size());
    }

    @Then("the report project slug should be {string}")
    public void reportProjectSlugShouldBe(String expectedSlug) {
        assertNotNull(report);
        assertEquals(expectedSlug, report.getProjectSlug());
    }

    @Then("every result should have a score between {double} and {double}")
    public void everyResultShouldHaveScoreBetween(double min, double max) {
        assertNotNull(report);
        assertFalse(report.getResults().isEmpty());
        for (AnalysisResult r : report.getResults()) {
            assertTrue(r.getScore() >= min && r.getScore() <= max,
                    "Score " + r.getScore() + " not in range [" + min + ", " + max + "]");
        }
    }

    @Then("every result should have a verdict of AUTOMATABLE, MAYBE, or NOT_SUITABLE")
    public void everyResultShouldHaveVerdict() {
        assertNotNull(report);
        for (AnalysisResult r : report.getResults()) {
            assertNotNull(r.getVerdict());
            assertTrue(
                    r.getVerdict() == Verdict.AUTOMATABLE
                            || r.getVerdict() == Verdict.MAYBE
                            || r.getVerdict() == Verdict.NOT_SUITABLE,
                    "Unexpected verdict: " + r.getVerdict());
        }
    }

    @Then("every result should have a non-empty list of reasons")
    public void everyResultShouldHaveReasons() {
        assertNotNull(report);
        for (AnalysisResult r : report.getResults()) {
            assertNotNull(r.getReasons(), "Reasons should not be null");
            assertFalse(r.getReasons().isEmpty(), "Reasons should not be empty");
        }
    }

    @Then("every result should have a non-empty list of matched rules")
    public void everyResultShouldHaveMatchedRules() {
        assertNotNull(report);
        for (AnalysisResult r : report.getResults()) {
            assertNotNull(r.getMatchedRules(), "Matched rules should not be null");
            assertFalse(r.getMatchedRules().isEmpty(), "Matched rules should not be empty");
        }
    }

    @Then("the system should return an error response")
    public void systemShouldReturnError() {
        assertNotNull(caughtException, "Expected an exception to be thrown");
    }

    @Then("every result should have an LLM comment containing {string}")
    public void everyResultShouldHaveLlmComment(String expectedText) {
        assertNotNull(report);
        for (AnalysisResult r : report.getResults()) {
            assertNotNull(r.getLlmComment(), "LLM comment should not be null");
            assertTrue(r.getLlmComment().contains(expectedText),
                    "Expected LLM comment to contain '" + expectedText + "' but was: " + r.getLlmComment());
        }
    }

    @Then("every result should have no LLM comment")
    public void everyResultShouldHaveNoLlmComment() {
        assertNotNull(report);
        for (AnalysisResult r : report.getResults()) {
            assertTrue(r.getLlmComment() == null || r.getLlmComment().isBlank(),
                    "Expected no LLM comment but was: " + r.getLlmComment());
        }
    }

    @When("I trigger analysis with a blank project slug")
    public void triggerAnalysisWithBlankSlug() {
        try {
            FetchCriteria criteria = FetchCriteria.builder()
                    .projectSlug("")
                    .state("merged")
                    .limit(100)
                    .build();
            report = analyzeMrUseCase.analyze(criteria, false, List.of());
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the system should return a validation error about project slug")
    public void systemShouldReturnValidationError() {
        assertNotNull(caughtException, "Expected a validation exception");
        assertTrue(caughtException instanceof InvalidRequestException
                        || caughtException instanceof IllegalArgumentException,
                "Expected InvalidRequestException or IllegalArgumentException but got: "
                        + caughtException.getClass().getName());
        assertTrue(caughtException.getMessage().toLowerCase().contains("projectslug")
                        || caughtException.getMessage().toLowerCase().contains("project"),
                "Error message should mention projectSlug, but was: " + caughtException.getMessage());
    }

    // --- Helpers ---

    private MergeRequest buildTestMr(int index) {
        return MergeRequest.builder()
                .id((long) index)
                .externalId(String.valueOf(index))
                .title("PR #" + index + " - refactor module")
                .description("Refactoring module " + index + " for better readability")
                .author("developer" + index)
                .sourceBranch("feature/pr-" + index)
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(index))
                .mergedAt(LocalDateTime.now().minusDays(index).plusHours(2))
                .labels(List.of())
                .changedFiles(List.of(
                        new ChangedFile("src/main/Module" + index + ".java", 20, 5, "modified"),
                        new ChangedFile("src/test/Module" + index + "Test.java", 15, 2, "modified"),
                        new ChangedFile("src/main/Helper" + index + ".java", 10, 3, "modified"),
                        new ChangedFile("src/main/Service" + index + ".java", 8, 4, "modified"),
                        new ChangedFile("src/main/Config" + index + ".java", 5, 1, "modified")
                ))
                .diffStats(new DiffStats(58, 15, 5))
                .hasTests(true)
                .ciPassed(true)
                .approvalsCount(2)
                .commentsCount(3)
                .provider("github")
                .url("https://github.com/owner/repo/pull/" + index)
                .projectSlug("owner/repo")
                .build();
    }
}
