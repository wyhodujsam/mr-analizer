package com.mranalizer.domain.model;

/**
 * Aggregate statistics for the diff of a merge request.
 */
public record DiffStats(
        int additions,
        int deletions,
        int changedFilesCount
) {}
