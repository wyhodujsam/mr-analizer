package com.mranalizer.domain.model;

/**
 * Represents a single file changed within a merge request.
 * status: added, modified, deleted, renamed
 */
public record ChangedFile(
        String path,
        int additions,
        int deletions,
        String status
) {}
