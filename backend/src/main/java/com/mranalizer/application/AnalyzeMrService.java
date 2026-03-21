package com.mranalizer.application;

import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringEngine;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AnalyzeMrService implements AnalyzeMrUseCase {

    private final MergeRequestProvider provider;
    private final LlmAnalyzer llmAnalyzer;
    private final AnalysisResultRepository repository;
    private final ScoringEngine scoringEngine;
    private final List<Rule> rules;
    private final ManageReposUseCase manageReposUseCase;
    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(3);

    public AnalyzeMrService(MergeRequestProvider provider,
                            LlmAnalyzer llmAnalyzer,
                            AnalysisResultRepository repository,
                            ScoringEngine scoringEngine,
                            List<Rule> rules,
                            ManageReposUseCase manageReposUseCase) {
        this.provider = provider;
        this.llmAnalyzer = llmAnalyzer;
        this.repository = repository;
        this.scoringEngine = scoringEngine;
        this.rules = rules;
        this.manageReposUseCase = manageReposUseCase;
    }

    @Override
    public AnalysisReport analyze(FetchCriteria criteria, boolean useLlm, List<String> selectedMrIds) {
        if (criteria.getProjectSlug() == null || criteria.getProjectSlug().isBlank()) {
            throw new InvalidRequestException("projectSlug is required");
        }

        List<MergeRequest> mergeRequests;
        if (selectedMrIds != null && !selectedMrIds.isEmpty()) {
            // Fetch only selected MRs individually (avoid fetching all + files for unselected)
            mergeRequests = selectedMrIds.stream()
                    .map(mrId -> provider.fetchMergeRequest(criteria.getProjectSlug(), mrId))
                    .toList();
        } else {
            mergeRequests = provider.fetchMergeRequests(criteria);
        }

        List<AnalysisResult> results;
        if (useLlm) {
            // Parallel LLM analysis (max 3 concurrent)
            List<CompletableFuture<AnalysisResult>> futures = mergeRequests.stream()
                    .map(mr -> CompletableFuture.supplyAsync(() -> {
                        LlmAssessment llmAssessment;
                        try {
                            llmAssessment = llmAnalyzer.analyze(mr);
                        } catch (Exception e) {
                            llmAssessment = new LlmAssessment(0.0, "LLM error: " + e.getMessage(), "error");
                        }
                        return scoringEngine.evaluate(mr, rules, llmAssessment);
                    }, llmExecutor))
                    .toList();

            results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } else {
            // Sequential scoring without LLM
            results = mergeRequests.stream()
                    .map(mr -> {
                        LlmAssessment llmAssessment = new LlmAssessment(0.0, null, "none");
                        return scoringEngine.evaluate(mr, rules, llmAssessment);
                    })
                    .toList();
        }

        AnalysisReport report = AnalysisReport.of(
                null,
                criteria.getProjectSlug(),
                provider.getProviderName(),
                LocalDateTime.now(),
                results
        );

        AnalysisReport saved = repository.save(report);

        manageReposUseCase.add(criteria.getProjectSlug(), provider.getProviderName());

        return saved;
    }

    @Override
    public void deleteAnalysis(Long reportId) {
        repository.deleteById(reportId);
    }

    @PreDestroy
    void shutdown() {
        llmExecutor.shutdownNow();
    }
}
