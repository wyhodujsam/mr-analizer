package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.model.activity.ActivityRepoCache;
import com.mranalizer.domain.model.project.*;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.rules.BoostRule;
import com.mranalizer.domain.rules.PenalizeRule;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import com.mranalizer.domain.service.activity.ActivityAnalysisService;
import com.mranalizer.domain.service.activity.MetricsCalculator;
import com.mranalizer.domain.service.activity.rules.AggregateRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ProjectAnalysisServiceTest {

    private MergeRequestProvider mrProvider;
    private LlmAnalyzer llmAnalyzer;
    private ProjectAnalysisRepository repository;
    private ProjectAnalysisService service;

    @BeforeEach
    void setUp() {
        mrProvider = Mockito.mock(MergeRequestProvider.class);
        ReviewProvider reviewProvider = Mockito.mock(ReviewProvider.class);
        llmAnalyzer = Mockito.mock(LlmAnalyzer.class);
        repository = Mockito.mock(ProjectAnalysisRepository.class);
        Mockito.when(repository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        ActivityAnalysisService activityService = new ActivityAnalysisService(
                mrProvider, reviewProvider, List.of(), new AggregateRules(),
                new MetricsCalculator(), Duration.ofHours(1));

        ScoringEngine scoringEngine = new ScoringEngine(new ScoringConfig(0.5, 0.7, 0.4));
        List<Rule> rules = List.of(
                BoostRule.byHasTests(0.15),
                PenalizeRule.byLargeDiff(500, -0.2),
                PenalizeRule.byNoDescription(-0.3)
        );

        ArtifactDetector detector = new ArtifactDetector(new DetectionPatterns(
                List.of("*.feature", "*Steps.java"),
                List.of("spec.md", "plan.md", "tasks.md")
        ));

        service = new ProjectAnalysisService(
                activityService, mrProvider, scoringEngine, rules, detector, llmAnalyzer, repository);
    }

    private MergeRequest buildMr(String id, String author, int add, int del,
                                  String state, String desc) {
        return MergeRequest.builder()
                .externalId(id).title("PR #" + id).author(author)
                .description(desc).state(state)
                .diffStats(new DiffStats(add, del, 3))
                .createdAt(LocalDateTime.of(2026, 3, 10, 10, 0))
                .mergedAt("merged".equals(state) ? LocalDateTime.of(2026, 3, 10, 12, 0) : null)
                .updatedAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .build();
    }

    private void setupMocks(List<MergeRequest> prs) {
        when(mrProvider.fetchMergeRequests(any())).thenReturn(prs);
        for (MergeRequest mr : prs) {
            when(mrProvider.fetchMergeRequest(anyString(), eq(mr.getExternalId()))).thenReturn(mr);
        }
    }

    @Test
    void shouldAnalyzeProjectWithBddAndSdd() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "alice", 100, 20, "merged", "Add login"),
                buildMr("2", "bob", 50, 10, "merged", "Fix bug"),
                buildMr("3", "alice", 200, 30, "merged", "Add tests")
        );
        setupMocks(prs);

        // PR 1: has BDD files
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenReturn(List.of(
                new ChangedFile("src/test/features/login.feature", 20, 0, "added"),
                new ChangedFile("src/main/java/LoginService.java", 80, 20, "modified")
        ));
        // PR 2: has SDD files
        when(mrProvider.fetchFiles(anyString(), eq("2"))).thenReturn(List.of(
                new ChangedFile("specs/005/spec.md", 50, 0, "added"),
                new ChangedFile("specs/005/plan.md", 30, 0, "added")
        ));
        // PR 3: has both
        when(mrProvider.fetchFiles(anyString(), eq("3"))).thenReturn(List.of(
                new ChangedFile("src/test/features/scoring.feature", 30, 0, "added"),
                new ChangedFile("specs/006/tasks.md", 20, 0, "added"),
                new ChangedFile("src/test/java/ScoringSteps.java", 40, 0, "added")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        assertEquals(3, result.getRows().size());
        assertEquals(3, result.getSummary().totalPrs());

        // BDD: PR 1 and PR 3
        assertTrue(result.getRows().get(0).hasBdd());
        assertFalse(result.getRows().get(1).hasBdd());
        assertTrue(result.getRows().get(2).hasBdd());
        assertEquals(2, result.getSummary().bddCount());

        // SDD: PR 2 and PR 3
        assertFalse(result.getRows().get(0).hasSdd());
        assertTrue(result.getRows().get(1).hasSdd());
        assertTrue(result.getRows().get(2).hasSdd());
        assertEquals(2, result.getSummary().sddCount());
    }

    @Test
    void shouldComputeAiScoresAndSummary() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "alice", 100, 20, "merged", "Small fix with tests"),
                buildMr("2", "bob", 800, 200, "merged", null) // large + no description
        );
        setupMocks(prs);

        // PR 1: has test files → boost
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenReturn(List.of(
                new ChangedFile("src/test/java/ServiceTest.java", 50, 0, "added"),
                new ChangedFile("src/main/java/Service.java", 50, 20, "modified")
        ));
        // PR 2: no test files, large diff
        when(mrProvider.fetchFiles(anyString(), eq("2"))).thenReturn(List.of(
                new ChangedFile("src/main/java/BigRefactor.java", 800, 200, "modified")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        // PR 1 should score higher (has tests boost)
        assertTrue(result.getRows().get(0).aiScore() > result.getRows().get(1).aiScore());

        // Summary
        assertTrue(result.getSummary().avgScore() > 0);
        assertEquals(5, result.getSummary().histogram().size());
        assertFalse(result.getSummary().topRules().isEmpty());
    }

    @Test
    void shouldIncludeRuleResultsForDrillDown() {
        // PR with tests (boost) + large diff (penalize) → at least 2 matched rules
        List<MergeRequest> prs = List.of(buildMr("1", "alice", 600, 100, "merged", "Fix bug"));
        setupMocks(prs);
        when(mrProvider.fetchFiles(anyString(), eq("1"))).thenReturn(List.of(
                new ChangedFile("src/test/java/ServiceTest.java", 300, 0, "added"),
                new ChangedFile("src/main/java/Service.java", 300, 100, "modified")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        PrAnalysisRow row = result.getRows().get(0);
        assertNotNull(row.ruleResults());
        assertFalse(row.ruleResults().isEmpty());
        row.ruleResults().forEach(rr -> {
            assertNotNull(rr.ruleName());
            assertTrue(rr.weight() != 0);
        });
    }

    @Test
    void shouldHandleEmptyRepo() {
        setupMocks(List.of());

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        assertEquals(0, result.getRows().size());
        assertEquals(0, result.getSummary().totalPrs());
        assertEquals(0, result.getSummary().bddCount());
        assertEquals(0, result.getSummary().sddCount());
    }

    @Test
    void shouldComputeTopRulesAcrossAllPrs() {
        List<MergeRequest> prs = List.of(
                buildMr("1", "a", 100, 20, "merged", "Fix"),
                buildMr("2", "b", 100, 20, "merged", "Fix"),
                buildMr("3", "c", 100, 20, "merged", null) // no description → penalize
        );
        setupMocks(prs);
        when(mrProvider.fetchFiles(anyString(), anyString())).thenReturn(List.of(
                new ChangedFile("src/main/java/X.java", 100, 20, "modified")
        ));

        ProjectAnalysisResult result = service.analyzeProject("owner/repo", false);

        List<RuleFrequency> topRules = result.getSummary().topRules();
        assertFalse(topRules.isEmpty());
        // Should be sorted by match count desc
        for (int i = 0; i < topRules.size() - 1; i++) {
            assertTrue(topRules.get(i).matchCount() >= topRules.get(i + 1).matchCount());
        }
    }

    @Test
    void shouldNotUseLlmWhenFlagIsFalse() {
        List<MergeRequest> prs = List.of(buildMr("1", "a", 100, 20, "merged", "Fix"));
        setupMocks(prs);
        when(mrProvider.fetchFiles(anyString(), anyString())).thenReturn(List.of());

        service.analyzeProject("owner/repo", false);

        Mockito.verifyNoInteractions(llmAnalyzer);
    }
}
