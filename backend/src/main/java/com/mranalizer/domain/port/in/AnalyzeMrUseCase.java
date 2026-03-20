package com.mranalizer.domain.port.in;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.FetchCriteria;

/**
 * Inbound port: trigger analysis of merge requests matching the given criteria.
 * Pure domain — no framework dependencies.
 */
public interface AnalyzeMrUseCase {

    /**
     * Fetch MRs according to {@code criteria}, run all rules (and optionally LLM),
     * persist an {@link AnalysisReport} and return it.
     *
     * @param criteria filtering parameters for the VCS provider
     * @param useLlm   whether to invoke the LLM analyzer in addition to rule-based analysis
     * @return the persisted analysis report
     */
    AnalysisReport analyze(FetchCriteria criteria, boolean useLlm, java.util.List<String> selectedMrIds);

    /**
     * Delete a previously saved analysis report.
     *
     * @param reportId the report ID to delete
     */
    void deleteAnalysis(Long reportId);
}
