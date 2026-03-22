package com.mranalizer.adapter.in.rest.activity.dto;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ActivityReportResponse(
        String contributor,
        String projectSlug,
        StatsDto stats,
        List<FlagDto> flags,
        Map<String, DailyActivityDto> dailyActivity,
        List<PullRequestDto> pullRequests
) {

    public static ActivityReportResponse from(ActivityReport report) {
        StatsDto stats = new StatsDto(
                report.getStats().totalPrs(),
                report.getStats().avgSize(),
                report.getStats().avgReviewTimeMinutes(),
                report.getStats().weekendPercentage(),
                report.getStats().flagCountBySeverity().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue))
        );

        List<FlagDto> flags = report.getFlags().stream()
                .map(f -> new FlagDto(f.type().name(), f.type().getDisplayName(),
                        f.severity().name(), f.description(), f.prReference()))
                .toList();

        Map<String, DailyActivityDto> daily = report.getDailyActivity().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> new DailyActivityDto(e.getValue().count(),
                                e.getValue().pullRequests().stream()
                                        .map(pr -> new DailyPrDto(pr.id(), pr.title(), pr.size()))
                                        .toList())
                ));

        List<PullRequestDto> prs = report.getPullRequests().stream()
                .map(mr -> new PullRequestDto(
                        mr.getExternalId(), mr.getTitle(),
                        mr.getDiffStats().additions() + mr.getDiffStats().deletions(),
                        mr.getCreatedAt(), mr.getMergedAt(), mr.getState(),
                        report.getFlags().stream()
                                .filter(f -> ("#" + mr.getExternalId()).equals(f.prReference()))
                                .map(f -> new FlagDto(f.type().name(), f.type().getDisplayName(),
                                        f.severity().name(), f.description(), f.prReference()))
                                .toList()))
                .toList();

        return new ActivityReportResponse(report.getContributor(), report.getProjectSlug(),
                stats, flags, daily, prs);
    }

    public record StatsDto(
            int totalPrs,
            double avgSize,
            double avgReviewTimeMinutes,
            double weekendPercentage,
            Map<String, Long> flagCounts
    ) {}

    public record FlagDto(
            String type,
            String displayName,
            String severity,
            String description,
            String prReference
    ) {}

    public record DailyActivityDto(
            int count,
            List<DailyPrDto> pullRequests
    ) {}

    public record DailyPrDto(
            String id,
            String title,
            int size
    ) {}

    public record PullRequestDto(
            String id,
            String title,
            int size,
            LocalDateTime createdAt,
            LocalDateTime mergedAt,
            String state,
            List<FlagDto> flags
    ) {}
}
