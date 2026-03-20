package com.mranalizer.application.dto;

import java.time.LocalDate;

public record AnalysisRequestDto(
        String projectSlug,
        String provider,
        String targetBranch,
        String state,
        LocalDate after,
        LocalDate before,
        int limit,
        boolean useLlm,
        java.util.List<String> selectedMrIds
) {
    public AnalysisRequestDto {
        if (limit <= 0) limit = 100;
        if (state == null || state.isBlank()) state = "merged";
        if (provider == null || provider.isBlank()) provider = "github";
        if (selectedMrIds == null) selectedMrIds = java.util.List.of();
    }
}
