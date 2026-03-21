package com.mranalizer.domain.port.out;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence abstraction for {@link AnalysisReport}.
 * Pure domain — no framework dependencies.
 */
public interface AnalysisResultRepository {

    /**
     * Persists an analysis report. If the report has no ID the implementation
     * is responsible for assigning one.
     *
     * @param report the report to save
     * @return the saved report (possibly with an assigned ID)
     */
    AnalysisReport save(AnalysisReport report);

    /** Returns all persisted reports; never {@code null}. */
    List<AnalysisReport> findAll();

    /** Returns a report by its ID, or empty if none exists with that ID. */
    Optional<AnalysisReport> findById(Long id);

    /** Deletes a report by its ID. */
    void deleteById(Long id);

    /** Returns a single result by report ID and result ID, or empty if not found. */
    Optional<AnalysisResult> findResult(Long reportId, Long resultId);

    /** Returns all reports matching the given project slug. */
    List<AnalysisReport> findByProjectSlug(String projectSlug);
}
