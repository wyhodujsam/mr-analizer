package com.mranalizer.adapter.in.rest.dto;

import com.mranalizer.domain.model.MergeRequest;

import java.util.List;

public record MrBrowseResponse(
        String externalId,
        String title,
        String author,
        String createdAt,
        String mergedAt,
        String state,
        int changedFilesCount,
        List<String> labels,
        String url
) {
    public static MrBrowseResponse from(MergeRequest mr) {
        return new MrBrowseResponse(
                mr.getExternalId(),
                mr.getTitle(),
                mr.getAuthor(),
                mr.getCreatedAt() != null ? mr.getCreatedAt().toString() : null,
                mr.getMergedAt() != null ? mr.getMergedAt().toString() : null,
                mr.getState(),
                mr.getChangedFiles() != null ? mr.getChangedFiles().size() : 0,
                mr.getLabels() != null ? mr.getLabels() : List.of(),
                mr.getUrl()
        );
    }
}
