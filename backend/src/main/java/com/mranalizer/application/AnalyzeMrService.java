package com.mranalizer.application;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
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
public class AnalyzeMrService implements AnalyzeMrUseCase, GetAnalysisResultsUseCase {

    private final MergeRequestProvider provider;
    private final LlmAnalyzer llmAnalyzer;
    private final AnalysisResultRepository repository;
    private final ScoringEngine scoringEngine;
    private final List<Rule> rules;

    public AnalyzeMrService(MergeRequestProvider provider,
                            LlmAnalyzer llmAnalyzer,
                            AnalysisResultRepository repository,
                            ScoringEngine scoringEngine,
                            List<Rule> rules) {
        this.provider = provider;
        this.llmAnalyzer = llmAnalyzer;
        this.repository = repository;
        this.scoringEngine = scoringEngine;
        this.rules = rules;
    }

    @Override
    public AnalysisReport analyze(FetchCriteria criteria, boolean useLlm) {
        List<MergeRequest> mergeRequests = provider.fetchMergeRequests(criteria);

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

        return repository.save(report);
    }

    @Override
    public List<AnalysisReport> getAllReports() {
        return repository.findAll();
    }

    @Override
    public Optional<AnalysisReport> getReport(Long reportId) {
        return repository.findById(reportId);
    }

    @Override
    public Optional<AnalysisResult> getResult(Long reportId, Long resultId) {
        return repository.findById(reportId)
                .flatMap(report -> report.getResults().stream()
                        .filter(r -> resultId.equals(r.getId()))
                        .findFirst());
    }
}
