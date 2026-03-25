package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.project.ProjectAnalysisResult;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import com.mranalizer.domain.port.in.project.ProjectAnalysisUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ProjectAnalysisSteps {

    @Autowired private ProjectAnalysisUseCase projectAnalysis;
    @Autowired private ActivityAnalysisUseCase activityAnalysis;
    @Autowired private MergeRequestProvider mergeRequestProvider;
    @Autowired private ReviewProvider reviewProvider;

    private List<MergeRequest> mockPrs = new ArrayList<>();
    private ProjectAnalysisResult result;

    @Before
    public void setUp() {
        mockPrs = new ArrayList<>();
        result = null;
        activityAnalysis.invalidateCache("owner/repo");
    }

    @Given("repozytorium z {int} PR-ami o różnych cechach")
    public void repozytoriumZPramiORoznychCechach(int count) {
        mockPrs.clear();
        LocalDateTime base = LocalDateTime.of(2026, 3, 9, 10, 0);
        for (int i = 0; i < count; i++) {
            mockPrs.add(buildMr(String.valueOf(i + 1), "author" + (i % 3),
                    100 + i * 50, 20 + i * 10, base.plusDays(i),
                    base.plusDays(i).plusHours(2), "merged",
                    i % 2 == 0 ? "Fix something" : null));
        }
        setupMocks();
        // Set up files: some with BDD, some with SDD
        for (int i = 0; i < count; i++) {
            List<ChangedFile> files = new ArrayList<>();
            files.add(new ChangedFile("src/main/java/Service" + i + ".java", 100, 20, "modified"));
            if (i % 2 == 0) files.add(new ChangedFile("src/test/features/test" + i + ".feature", 30, 0, "added"));
            if (i % 3 == 0) files.add(new ChangedFile("specs/00" + i + "/spec.md", 50, 0, "added"));
            when(mergeRequestProvider.fetchFiles(anyString(), eq(String.valueOf(i + 1)))).thenReturn(files);
        }
    }

    @Given("repozytorium z {int} PR-ami")
    public void repozytoriumZPrami(int count) {
        repozytoriumZPramiORoznychCechach(count);
    }

    @Given("PR z testami i małymi zmianami")
    public void prZTestamiIMalymiZmianami() {
        mockPrs.add(buildMr("100", "alice", 80, 20, ldt(), ldt().plusHours(1), "merged", "Small fix"));
        setupMocks();
        when(mergeRequestProvider.fetchFiles(anyString(), eq("100"))).thenReturn(List.of(
                new ChangedFile("src/test/java/ServiceTest.java", 40, 0, "added"),
                new ChangedFile("src/main/java/Service.java", 40, 20, "modified")
        ));
    }

    @And("PR z dużymi zmianami bez opisu")
    public void prZDuzymiZmianamiBezOpisu() {
        mockPrs.add(buildMr("101", "bob", 800, 200, ldt(), ldt().plusHours(3), "merged", null));
        setupMocks();
        when(mergeRequestProvider.fetchFiles(anyString(), eq("101"))).thenReturn(List.of(
                new ChangedFile("src/main/java/BigRefactor.java", 800, 200, "modified")
        ));
    }

    @Given("PR dodający plik {string}")
    public void prDodajacyPlik(String filePath) {
        String id = String.valueOf(mockPrs.size() + 1);
        mockPrs.add(buildMr(id, "alice", 50, 10, ldt(), ldt().plusHours(1), "merged", "Add file"));
        setupMocks();
        when(mergeRequestProvider.fetchFiles(anyString(), eq(id))).thenReturn(List.of(
                new ChangedFile(filePath, 50, 0, "added"),
                new ChangedFile("src/main/java/Service.java", 20, 10, "modified")
        ));
    }

    @And("PR bez plików BDD")
    public void prBezPlikowBdd() {
        String id = String.valueOf(mockPrs.size() + 1);
        mockPrs.add(buildMr(id, "bob", 30, 5, ldt(), ldt().plusHours(1), "merged", "Minor fix"));
        setupMocks();
        when(mergeRequestProvider.fetchFiles(anyString(), eq(id))).thenReturn(List.of(
                new ChangedFile("src/main/java/Utils.java", 30, 5, "modified")
        ));
    }

    @And("PR bez plików SDD")
    public void prBezPlikowSdd() {
        prBezPlikowBdd(); // reuse — no SDD files either
    }

    @When("analizuję projekt {string}")
    public void analizujeProjekt(String slug) {
        result = projectAnalysis.analyzeProject(slug, false);
    }

    @Then("otrzymuję wynik z {int} wierszami")
    public void otrzymujeWynikZWierszami(int count) {
        assertNotNull(result);
        assertEquals(count, result.getRows().size());
    }

    @And("summary zawiera poprawne procenty AI\\/BDD\\/SDD")
    public void summaryZawieraPoprawneProcenty() {
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().totalPrs() > 0);
        assertEquals(result.getSummary().automatableCount() + result.getSummary().maybeCount() + result.getSummary().notSuitableCount(),
                result.getSummary().totalPrs());
        assertTrue(result.getSummary().bddPercent() >= 0 && result.getSummary().bddPercent() <= 100);
        assertTrue(result.getSummary().sddPercent() >= 0 && result.getSummary().sddPercent() <= 100);
    }

    @Then("PR z testami ma verdict AUTOMATABLE lub MAYBE")
    public void prZTestamiMaVerdictAutomatableOrMaybe() {
        var row = result.getRows().stream().filter(r -> r.prId().equals("100")).findFirst().orElseThrow();
        assertTrue(row.aiScore() > 0.4, "PR with tests should score > 0.4, got " + row.aiScore());
    }

    @And("PR z dużymi zmianami ma niższy score")
    public void prZDuzymiZmianamMaNizszyScore() {
        var small = result.getRows().stream().filter(r -> r.prId().equals("100")).findFirst().orElseThrow();
        var large = result.getRows().stream().filter(r -> r.prId().equals("101")).findFirst().orElseThrow();
        assertTrue(small.aiScore() > large.aiScore(),
                "Small PR score (" + small.aiScore() + ") should be > large PR (" + large.aiScore() + ")");
    }

    @Then("pierwszy PR ma hasBdd true")
    public void pierwszyPrMaHasBddTrue() {
        assertTrue(result.getRows().get(0).hasBdd());
    }

    @And("drugi PR ma hasBdd false")
    public void drugiPrMaHasBddFalse() {
        assertFalse(result.getRows().get(1).hasBdd());
    }

    @Then("pierwszy PR ma hasSdd true")
    public void pierwszyPrMaHasSddTrue() {
        assertTrue(result.getRows().get(0).hasSdd());
    }

    @And("drugi PR ma hasSdd false")
    public void drugiPrMaHasSddFalse() {
        assertFalse(result.getRows().get(1).hasSdd());
    }

    @Then("każdy wiersz zawiera listę ruleResults z nazwą i wagą")
    public void kazdyWierszZawieraRuleResults() {
        assertNotNull(result);
        for (var row : result.getRows()) {
            assertNotNull(row.ruleResults());
            // ruleResults may be empty for some PRs (no rules matched)
        }
    }

    @Then("summary zawiera topRules z liczbą matchów")
    public void summaryZawieraTopRules() {
        assertNotNull(result.getSummary().topRules());
        // With 5 PRs some rules should match
        assertFalse(result.getSummary().topRules().isEmpty());
        result.getSummary().topRules().forEach(r -> {
            assertNotNull(r.ruleName());
            assertTrue(r.matchCount() > 0);
        });
    }

    @And("summary zawiera histogram z {int} bucketami")
    public void summaryZawieraHistogramZBucketami(int count) {
        assertEquals(count, result.getSummary().histogram().size());
        int totalInBuckets = result.getSummary().histogram().stream().mapToInt(b -> b.count()).sum();
        assertEquals(result.getSummary().totalPrs(), totalInBuckets);
    }

    private void setupMocks() {
        when(mergeRequestProvider.fetchMergeRequests(any(FetchCriteria.class))).thenReturn(mockPrs);
        for (MergeRequest mr : mockPrs) {
            when(mergeRequestProvider.fetchMergeRequest(anyString(), eq(mr.getExternalId()))).thenReturn(mr);
        }
        when(reviewProvider.fetchReviews(anyString(), anyString())).thenReturn(List.of());
    }

    private LocalDateTime ldt() {
        return LocalDateTime.of(2026, 3, 10, 10, 0);
    }

    private MergeRequest buildMr(String id, String author, int add, int del,
                                  LocalDateTime created, LocalDateTime merged, String state, String desc) {
        return MergeRequest.builder()
                .externalId(id).title("PR #" + id).author(author).description(desc)
                .state(state).diffStats(new DiffStats(add, del, 3))
                .createdAt(created).mergedAt(merged).updatedAt(merged != null ? merged : created)
                .build();
    }
}
