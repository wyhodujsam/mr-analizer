package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.AnalysisReport;
import org.springframework.stereotype.Component;

/**
 * Shared mutable state between step definition classes within a single Cucumber scenario.
 * Scoped per-scenario by Cucumber's Spring integration (each scenario gets a fresh context).
 */
@Component
public class ScenarioContext {

    private AnalysisReport lastAnalysisReport;

    public AnalysisReport getLastAnalysisReport() {
        return lastAnalysisReport;
    }

    public void setLastAnalysisReport(AnalysisReport report) {
        this.lastAnalysisReport = report;
    }

    public void clear() {
        lastAnalysisReport = null;
    }
}
