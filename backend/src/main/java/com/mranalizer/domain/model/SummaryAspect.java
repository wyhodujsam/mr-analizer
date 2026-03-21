package com.mranalizer.domain.model;

/**
 * A summary aspect of the analysis (e.g. "Code execution" -> 95% automatable).
 * Score may be null for aspects that are not quantifiable.
 */
public record SummaryAspect(
        String aspect,
        Integer score,
        String note
) {}
