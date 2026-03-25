package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.project.DetectionPatterns;
import com.mranalizer.domain.model.project.ProjectAnalysisResult;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.rules.BoostRule;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import com.mranalizer.domain.service.activity.ActivityAnalysisService;
import com.mranalizer.domain.service.activity.MetricsCalculator;
import com.mranalizer.domain.service.activity.rules.AggregateRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ProjectAnalysisServiceEnrichTest {

    private ProjectAnalysisService service;
    private ActivityAnalysisService activityService;
    private MergeRequestProvider mrProvider;

    @BeforeEach
    void setUp() {
        mrProvider = Mockito.mock(MergeRequestProvider.class);
        ReviewProvider reviewProvider = Mockito.mock(ReviewProvider.class);
        LlmAnalyzer llmAnalyzer = Mockito.mock(LlmAnalyzer.class);
        ProjectAnalysisRepository repository = Mockito.mock(ProjectAnalysisRepository.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        activityService = new ActivityAnalysisService(
                mrProvider, reviewProvider, List.of(), new AggregateRules(),
                new MetricsCalculator(), Duration.ofHours(1));

        ScoringEngine scoringEngine = new ScoringEngine(new ScoringConfig(0.5, 0.7, 0.4));
        List<Rule> rules = List.of(BoostRule.byHasTests(0.15));
        ArtifactDetector detector = new ArtifactDetector(new DetectionPatterns(List.of("*.feature"), List.of()));

        service = new ProjectAnalysisService(
                activityService, mrProvider, scoringEngine, rules, detector, llmAnalyzer, repository);

        activityService.invalidateCache("owner/repo");
    }

    @Test
    void enrichWithFiles_setsHasTestsTrue_whenTestFilesPresent() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1").title("Add auth").author("alice").state("merged")
                .description("Add auth module").sourceBranch("feature").targetBranch("main")
                .labels(List.of("enhancement"))
                .diffStats(new DiffStats(100, 20, 3))
                .createdAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .mergedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .provider("github").url("https://github.com/test/1").projectSlug("owner/repo")
                .build();

        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of(mr));
        when(mrProvider.fetchMergeRequest(anyString(), eq("1"))).thenReturn(mr);
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenReturn(List.of(
                new ChangedFile("src/test/java/AuthTest.java", 50, 0, "added"),
                new ChangedFile("src/main/java/Auth.java", 50, 20, "modified")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        var row = result.getRows().get(0);
        // enrichWithFiles detects "test" in file path → hasTests=true → boost applies → score > base 0.5
        assertTrue(row.aiScore() > 0.5,
                "PR with test files should score above base (0.5), got " + row.aiScore());
    }

    @Test
    void enrichWithFiles_setsHasTestsFalse_whenNoTestFiles() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1").title("Config change").author("bob").state("merged")
                .diffStats(new DiffStats(10, 5, 1))
                .createdAt(LocalDateTime.now()).mergedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of(mr));
        when(mrProvider.fetchMergeRequest(anyString(), eq("1"))).thenReturn(mr);
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenReturn(List.of(
                new ChangedFile("config/app.yml", 10, 5, "modified")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        var row = result.getRows().get(0);
        assertFalse(row.ruleResults().stream().anyMatch(r -> r.ruleName().contains("hasTests")),
                "Should NOT detect test files");
    }

    @Test
    void fetchFilesFailure_gracefulFallback() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1").title("PR").author("alice").state("merged")
                .diffStats(new DiffStats(100, 20, 3))
                .createdAt(LocalDateTime.now()).mergedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of(mr));
        when(mrProvider.fetchMergeRequest(anyString(), eq("1"))).thenReturn(mr);
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenThrow(new RuntimeException("API error"));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        assertEquals(1, result.getRows().size());
        assertFalse(result.getRows().get(0).hasBdd());
        assertFalse(result.getRows().get(0).hasSdd());
    }

    @Test
    void emptyRepo_returnsEmptyResult() {
        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of());

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        assertEquals(0, result.getRows().size());
        assertEquals(0, result.getSummary().totalPrs());
        assertEquals(0, result.getSummary().bddCount());
        assertEquals(0, result.getSummary().sddCount());
    }

    @Test
    void progressCallback_calledForEachPr() {
        MergeRequest pr1 = MergeRequest.builder().externalId("1").title("PR1").author("a").state("merged")
                .diffStats(new DiffStats(10, 5, 1)).createdAt(LocalDateTime.now())
                .mergedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        MergeRequest pr2 = MergeRequest.builder().externalId("2").title("PR2").author("b").state("merged")
                .diffStats(new DiffStats(10, 5, 1)).createdAt(LocalDateTime.now())
                .mergedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of(pr1, pr2));
        when(mrProvider.fetchMergeRequest(anyString(), anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(1);
            return "1".equals(id) ? pr1 : pr2;
        });
        when(mrProvider.fetchFiles(anyString(), anyString())).thenReturn(List.of());

        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.Set<Integer> totals = java.util.concurrent.ConcurrentHashMap.newKeySet();

        service.analyzeProject("owner/repo", false, (processed, total) -> {
            callCount.incrementAndGet();
            totals.add(total);
        });

        assertEquals(2, callCount.get());
        assertEquals(1, totals.size());
        assertTrue(totals.contains(2));
    }
}
