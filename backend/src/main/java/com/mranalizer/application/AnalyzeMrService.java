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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AnalyzeMrService implements AnalyzeMrUseCase {

    private final MergeRequestProvider provider;
    private final LlmAnalyzer llmAnalyzer;
    private final AnalysisResultRepository repository;
    private final ScoringEngine scoringEngine;
    private final List<Rule> rules;
    private final ManageReposUseCase manageReposUseCase;

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

        Optional<AnalysisReport> cached = repository.findByProjectSlug(criteria.getProjectSlug());
        if (cached.isPresent()) {
            return cached.get();
        }

        List<MergeRequest> mergeRequests = provider.fetchMergeRequests(criteria);

        if (selectedMrIds != null && !selectedMrIds.isEmpty()) {
            mergeRequests = mergeRequests.stream()
                    .filter(mr -> selectedMrIds.contains(mr.getExternalId()))
                    .toList();
        }

        List<AnalysisResult> results = new ArrayList<>();
        for (MergeRequest mr : mergeRequests) {
            LlmAssessment llmAssessment;
            if (useLlm) {
                try {
                    llmAssessment = llmAnalyzer.analyze(mr);
                } catch (Exception e) {
                    llmAssessment = new LlmAssessment(0.0, "LLM error: " + e.getMessage(), "error");
                }
            } else {
                llmAssessment = new LlmAssessment(0.0, null, "none");
            }

            AnalysisResult result = scoringEngine.evaluate(mr, rules, llmAssessment);
            results.add(result);
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
}
