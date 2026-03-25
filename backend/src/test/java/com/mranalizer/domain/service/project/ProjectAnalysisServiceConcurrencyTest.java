package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.project.DetectionPatterns;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ProjectAnalysisServiceConcurrencyTest {

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
        ArtifactDetector detector = new ArtifactDetector(new DetectionPatterns(List.of(), List.of()));

        service = new ProjectAnalysisService(
                activityService, mrProvider, scoringEngine, List.of(), detector, llmAnalyzer, repository);
    }

    private void invalidateCaches() {
        activityService.invalidateCache("owner/repo");
        activityService.invalidateCache("owner/repo1");
        activityService.invalidateCache("owner/repo2");
    }

    private void setupSlowMocks() {
        invalidateCaches();
        MergeRequest pr = MergeRequest.builder()
                .externalId("1").title("PR").author("alice").state("merged")
                .diffStats(new DiffStats(100, 20, 3))
                .createdAt(LocalDateTime.now()).mergedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        when(mrProvider.fetchMergeRequests(any())).thenReturn(List.of(pr));
        when(mrProvider.fetchMergeRequest(anyString(), anyString())).thenReturn(pr);
        // Slow fetchFiles to simulate long-running analysis
        when(mrProvider.fetchFiles(anyString(), anyString())).thenAnswer(inv -> {
            Thread.sleep(500);
            return List.of(new ChangedFile("file.java", 100, 20, "modified"));
        });
    }

    @Test
    void concurrentAnalysisSameSlug_throwsException() {
        setupSlowMocks();

        // Start first analysis in background
        CompletableFuture<Void> first = CompletableFuture.runAsync(
                () -> service.analyzeProject("owner/repo", false));

        // Wait a bit for first to start
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Second analysis on same slug should fail
        assertThrows(IllegalStateException.class,
                () -> service.analyzeProject("owner/repo", false));

        // Wait for first to finish
        first.join();
    }

    @Test
    void concurrentAnalysisDifferentSlug_succeeds() {
        setupSlowMocks();

        CompletableFuture<Void> first = CompletableFuture.runAsync(
                () -> service.analyzeProject("owner/repo1", false));

        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Different slug should work
        assertDoesNotThrow(() -> service.analyzeProject("owner/repo2", false));

        first.join();
    }

    @Test
    void afterFirstCompletes_sameSlugSucceeds() {
        setupSlowMocks();

        service.analyzeProject("owner/repo", false);

        // After completion, same slug should work again
        assertDoesNotThrow(() -> service.analyzeProject("owner/repo", false));
    }
}
