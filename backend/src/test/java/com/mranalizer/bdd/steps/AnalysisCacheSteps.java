package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnalysisCacheSteps {

    @Autowired
    private AnalyzeMrUseCase analyzeMrUseCase;

    @Autowired
    private GetAnalysisResultsUseCase getAnalysisResultsUseCase;

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    @Autowired
    private ScenarioContext scenarioContext;

    private AnalysisReport secondReport;

    @Before
    public void setUp() {
        secondReport = null;
    }

    // --- When steps ---

    @When("I trigger analysis for {string} again")
    public void triggerAnalysisAgain(String slug) {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .targetBranch("main")
                .state("merged")
                .limit(100)
                .build();
        secondReport = analyzeMrUseCase.analyze(criteria, false, List.of());
    }

    @When("I delete the analysis for {string}")
    public void deleteAnalysisFor(String slug) {
        AnalysisReport firstReport = scenarioContext.getLastAnalysisReport();
        assertNotNull(firstReport, "No analysis report exists to delete");
        analyzeMrUseCase.deleteAnalysis(firstReport.getId());
        scenarioContext.setLastAnalysisReport(null);
    }

    // --- Then steps ---

    @Then("the two analyses should have different report IDs")
    public void twoAnalysesShouldHaveDifferentReportIds() {
        AnalysisReport firstReport = scenarioContext.getLastAnalysisReport();
        assertNotNull(firstReport, "First report should exist");
        assertNotNull(secondReport, "Second report should exist");
        assertNotEquals(firstReport.getId(), secondReport.getId(),
                "Each analysis should produce a new report with a different ID");
    }

    @Then("the provider should have been called twice")
    public void providerShouldHaveBeenCalledTwice() {
        verify(mergeRequestProvider, times(2)).fetchMergeRequests(any());
    }

    @Then("the analysis history should contain {int} reports for {string}")
    public void analysisHistoryShouldContainNReportsFor(int expectedCount, String slug) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        long count = reports.stream().filter(r -> slug.equals(r.getProjectSlug())).count();
        assertEquals(expectedCount, count,
                "Expected " + expectedCount + " reports for '" + slug + "' but found " + count);
    }

    @Then("the analysis for {string} should not exist")
    public void analysisForShouldNotExist(String slug) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        assertTrue(reports.stream().noneMatch(r -> slug.equals(r.getProjectSlug())),
                "Expected no analysis for '" + slug + "' but found one");
    }

    @Then("the analysis history should contain a report for {string}")
    public void analysisHistoryShouldContainReportFor(String slug) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        assertTrue(reports.stream().anyMatch(r -> slug.equals(r.getProjectSlug())),
                "Expected analysis history to contain report for '" + slug + "'");
    }

    @Then("the report should have an analyzed date")
    public void reportShouldHaveAnalyzedDate() {
        AnalysisReport report = scenarioContext.getLastAnalysisReport();
        assertNotNull(report, "Report should exist");
        assertNotNull(report.getAnalyzedAt(), "Report should have an analyzed date");
    }

    @Then("the report should have {int} total MRs")
    public void reportShouldHaveTotalMrs(int expectedCount) {
        AnalysisReport report = scenarioContext.getLastAnalysisReport();
        assertNotNull(report, "Report should exist");
        assertEquals(expectedCount, report.getTotalMrs());
    }

    @When("I trigger analysis for {string} with selected MR ids {string}")
    public void triggerAnalysisWithSelectedIds(String slug, String idsStr) {
        List<String> selectedIds = List.of(idsStr.split(","));
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug(slug)
                .targetBranch("main")
                .state("merged")
                .limit(100)
                .build();
        AnalysisReport report = analyzeMrUseCase.analyze(criteria, false, selectedIds);
        scenarioContext.setLastAnalysisReport(report);
    }

    @Then("the selected analysis report should contain {int} results")
    public void selectedReportShouldContainResults(int count) {
        AnalysisReport report = scenarioContext.getLastAnalysisReport();
        assertNotNull(report);
        assertEquals(count, report.getResults().size());
    }

    @Then("the selected results should only include MR ids {string}")
    public void resultsShouldOnlyIncludeIds(String idsStr) {
        List<String> expectedIds = List.of(idsStr.split(","));
        AnalysisReport report = scenarioContext.getLastAnalysisReport();
        assertNotNull(report);
        List<String> actualIds = report.getResults().stream()
                .map(r -> r.getMergeRequest().getExternalId())
                .toList();
        assertEquals(expectedIds.size(), actualIds.size());
        assertTrue(actualIds.containsAll(expectedIds),
                "Expected MR ids " + expectedIds + " but got " + actualIds);
    }

    @Then("the analysis history should contain {int} PR entries")
    public void analysisHistoryShouldContainPrEntries(int expectedCount) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        long totalResults = reports.stream()
                .mapToLong(r -> r.getResults().size())
                .sum();
        assertEquals(expectedCount, totalResults,
                "Expected " + expectedCount + " PR entries but found " + totalResults);
    }

    @Then("each PR entry should have a title and score")
    public void eachPrEntryShouldHaveTitleAndScore() {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        for (AnalysisReport report : reports) {
            for (var result : report.getResults()) {
                assertNotNull(result.getMergeRequest().getTitle(),
                        "PR entry should have a title");
                assertFalse(result.getMergeRequest().getTitle().isBlank(),
                        "PR entry title should not be blank");
                assertTrue(result.getScore() >= 0,
                        "PR entry should have a score >= 0 but was " + result.getScore());
            }
        }
    }

    @Then("the analysis history should contain reports for {int} different repositories")
    public void analysisHistoryShouldContainReportsForDifferentRepos(int expectedCount) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        long uniqueSlugs = reports.stream()
                .map(AnalysisReport::getProjectSlug)
                .distinct()
                .count();
        assertEquals(expectedCount, uniqueSlugs,
                "Expected " + expectedCount + " different repositories but found " + uniqueSlugs);
    }

    @Then("the analysis history should contain {int} PR entries for {string}")
    public void analysisHistoryShouldContainPrEntriesForSlug(int expectedCount, String slug) {
        List<AnalysisReport> reports = getAnalysisResultsUseCase.getAllReports();
        long totalResults = reports.stream()
                .filter(r -> slug.equals(r.getProjectSlug()))
                .mapToLong(r -> r.getResults().size())
                .sum();
        assertEquals(expectedCount, totalResults,
                "Expected " + expectedCount + " PR entries for '" + slug + "' but found " + totalResults);
    }
}
