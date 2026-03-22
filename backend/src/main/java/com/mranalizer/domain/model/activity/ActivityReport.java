package com.mranalizer.domain.model.activity;

import com.mranalizer.domain.model.MergeRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ActivityReport {

    private final String contributor;
    private final String projectSlug;
    private final ContributorStats stats;
    private final List<ActivityFlag> flags;
    private final Map<LocalDate, DailyActivity> dailyActivity;
    private final List<MergeRequest> pullRequests;

    public ActivityReport(String contributor, String projectSlug, ContributorStats stats,
                          List<ActivityFlag> flags, Map<LocalDate, DailyActivity> dailyActivity,
                          List<MergeRequest> pullRequests) {
        this.contributor = contributor;
        this.projectSlug = projectSlug;
        this.stats = stats;
        this.flags = List.copyOf(flags);
        this.dailyActivity = Map.copyOf(dailyActivity);
        this.pullRequests = List.copyOf(pullRequests);
    }

    public String getContributor() { return contributor; }
    public String getProjectSlug() { return projectSlug; }
    public ContributorStats getStats() { return stats; }
    public List<ActivityFlag> getFlags() { return flags; }
    public Map<LocalDate, DailyActivity> getDailyActivity() { return dailyActivity; }
    public List<MergeRequest> getPullRequests() { return pullRequests; }

    public boolean hasFlags() { return !flags.isEmpty(); }
}
