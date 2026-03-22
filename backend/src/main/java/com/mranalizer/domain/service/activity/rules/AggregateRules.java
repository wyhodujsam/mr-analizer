package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.service.activity.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class AggregateRules {

    private static final double WEEKEND_RATIO_THRESHOLD = 0.30;

    public List<ActivityFlag> evaluate(List<MergeRequest> pullRequests) {
        if (pullRequests.isEmpty()) {
            return List.of();
        }

        List<ActivityFlag> flags = new ArrayList<>();

        // Only count PRs with known createdAt for both numerator and denominator
        List<MergeRequest> prsWithDate = pullRequests.stream()
                .filter(mr -> mr.getCreatedAt() != null)
                .toList();

        if (prsWithDate.isEmpty()) {
            return List.of();
        }

        long weekendCount = prsWithDate.stream()
                .filter(mr -> DateTimeUtils.isWeekend(mr.getCreatedAt()))
                .count();

        double ratio = (double) weekendCount / prsWithDate.size();
        if (ratio > WEEKEND_RATIO_THRESHOLD) {
            flags.add(new ActivityFlag(
                    FlagType.HIGH_WEEKEND_RATIO, Severity.WARNING,
                    String.format("%.0f%% PR-ów utworzonych w weekend (próg: %.0f%%)",
                            ratio * 100, WEEKEND_RATIO_THRESHOLD * 100),
                    null));
        }

        return flags;
    }
}
