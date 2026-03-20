package com.mranalizer.domain.port.out;

import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;

/**
 * Outbound port: abstracts access to an LLM provider for MR analysis.
 * Pure domain — no framework dependencies.
 */
public interface LlmAnalyzer {

    /**
     * Sends the merge request to the LLM and returns its structured assessment.
     *
     * @param mr the merge request to evaluate
     * @return the LLM's assessment
     */
    LlmAssessment analyze(MergeRequest mr);

    /** Returns the canonical name of this LLM provider (e.g. "openai", "anthropic"). */
    String getProviderName();
}
