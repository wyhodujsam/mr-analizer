package com.mranalizer.application.dto;

import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.FetchCriteria;

import java.time.LocalDate;
import java.util.List;

public record AnalysisRequestDto(
        String projectSlug,
        String provider,
        String targetBranch,
        String state,
        LocalDate after,
        LocalDate before,
        int limit,
        boolean useLlm,
        List<String> selectedMrIds
) {
    public AnalysisRequestDto {
        if (projectSlug == null || projectSlug.isBlank()) {
            throw new InvalidRequestException("projectSlug is required");
        }
        if (limit <= 0) limit = 100;
        if (state == null || state.isBlank()) state = "merged";
        if (provider == null || provider.isBlank()) provider = "github";
        if (selectedMrIds == null) selectedMrIds = List.of();
    }

    public FetchCriteria toFetchCriteria() {
        return FetchCriteria.builder()
                .projectSlug(projectSlug)
                .targetBranch(targetBranch)
                .state(state)
                .after(after)
                .before(before)
                .limit(limit)
                .build();
    }
}
