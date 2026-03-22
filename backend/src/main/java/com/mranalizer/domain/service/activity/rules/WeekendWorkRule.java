package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import com.mranalizer.domain.service.activity.DateTimeUtils;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class WeekendWorkRule implements ActivityRule {

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        List<ActivityFlag> flags = new ArrayList<>();

        if (DateTimeUtils.isWeekend(mr.getCreatedAt())) {
            flags.add(new ActivityFlag(
                    FlagType.WEEKEND_WORK, Severity.INFO,
                    "PR #" + mr.getExternalId() + " utworzony w " + polishDayName(mr.getCreatedAt().getDayOfWeek()),
                    "#" + mr.getExternalId()));
        }

        if (mr.getMergedAt() != null && DateTimeUtils.isWeekend(mr.getMergedAt())
                && !DateTimeUtils.isWeekend(mr.getCreatedAt())) {
            flags.add(new ActivityFlag(
                    FlagType.WEEKEND_WORK, Severity.INFO,
                    "PR #" + mr.getExternalId() + " zmergowany w " + polishDayName(mr.getMergedAt().getDayOfWeek()),
                    "#" + mr.getExternalId()));
        }

        return flags;
    }

    private String polishDayName(DayOfWeek day) {
        return switch (day) {
            case SATURDAY -> "sobotę";
            case SUNDAY -> "niedzielę";
            default -> day.name().toLowerCase();
        };
    }
}
