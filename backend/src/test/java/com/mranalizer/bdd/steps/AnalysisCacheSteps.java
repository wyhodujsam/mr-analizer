package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

    @Then("the second analysis should return the cached report")
    public void secondAnalysisShouldReturnCachedReport() {
        AnalysisReport firstReport = scenarioContext.getLastAnalysisReport();
        assertNotNull(firstReport, "First report should exist");
        assertNotNull(secondReport, "Second report should exist");
        assertEquals(firstReport.getId(), secondReport.getId(),
                "Cached report should have the same ID");
    }

    @Then("the provider should have been called only once")
    public void providerShouldHaveBeenCalledOnlyOnce() {
        verify(mergeRequestProvider, times(1)).fetchMergeRequests(any());
    }

    @Then("the provider should have been called twice")
    public void providerShouldHaveBeenCalledTwice() {
        verify(mergeRequestProvider, times(2)).fetchMergeRequests(any());
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
}
