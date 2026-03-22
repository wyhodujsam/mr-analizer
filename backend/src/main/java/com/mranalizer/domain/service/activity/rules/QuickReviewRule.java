package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class QuickReviewRule implements ActivityRule {

    private static final int MIN_SIZE_FOR_WARNING = 50;
    private static final int MIN_SIZE_FOR_CRITICAL = 100;
    private static final long WARNING_MINUTES = 10;
    private static final long CRITICAL_MINUTES = 5;

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        if (mr.getCreatedAt() == null || mr.getMergedAt() == null) {
            return List.of();
        }

        int totalLines = mr.getDiffStats().additions() + mr.getDiffStats().deletions();
        long minutes = Duration.between(mr.getCreatedAt(), mr.getMergedAt()).toMinutes();
        List<ActivityFlag> flags = new ArrayList<>();

        if (totalLines > MIN_SIZE_FOR_CRITICAL && minutes < CRITICAL_MINUTES) {
            flags.add(new ActivityFlag(
                    FlagType.SUSPICIOUS_QUICK_MERGE, Severity.CRITICAL,
                    "PR #" + mr.getExternalId() + " (" + totalLines + " linii) zmergowany w " + minutes + " min",
                    "#" + mr.getExternalId()));
        } else if (totalLines > MIN_SIZE_FOR_WARNING && minutes < WARNING_MINUTES) {
            flags.add(new ActivityFlag(
                    FlagType.QUICK_REVIEW, Severity.WARNING,
                    "PR #" + mr.getExternalId() + " (" + totalLines + " linii) zmergowany w " + minutes + " min",
                    "#" + mr.getExternalId()));
        }

        return flags;
    }
}
