package com.mranalizer.adapter.in.rest.dto;

import com.mranalizer.domain.model.SavedRepository;

public record SavedRepoResponse(
        Long id,
        String projectSlug,
        String provider,
        String addedAt,
        String lastAnalyzedAt
) {
    public static SavedRepoResponse from(SavedRepository repo) {
        return new SavedRepoResponse(
                repo.getId(),
                repo.getProjectSlug(),
                repo.getProvider(),
                repo.getAddedAt() != null ? repo.getAddedAt().toString() : null,
                repo.getLastAnalyzedAt() != null ? repo.getLastAnalyzedAt().toString() : null
        );
    }
}
