package com.mranalizer.domain.port.in;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: query previously stored analysis reports and results.
 * Pure domain — no framework dependencies.
 */
public interface GetAnalysisResultsUseCase {

    /** Returns all persisted analysis reports. */
    List<AnalysisReport> getAllReports();

    /** Returns a single report by its ID, or empty if not found. */
    Optional<AnalysisReport> getReport(Long reportId);

    /**
     * Returns a single result belonging to the given report, or empty if not found.
     *
     * @param reportId the parent report ID
     * @param resultId the result ID within that report
     */
    Optional<AnalysisResult> getResult(Long reportId, Long resultId);
}
