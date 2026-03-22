package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import com.mranalizer.domain.service.activity.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NightWorkRule implements ActivityRule {

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        List<ActivityFlag> flags = new ArrayList<>();

        if (DateTimeUtils.isNight(mr.getCreatedAt())) {
            flags.add(new ActivityFlag(
                    FlagType.NIGHT_WORK, Severity.INFO,
                    "PR #" + mr.getExternalId() + " utworzony o " + mr.getCreatedAt().toLocalTime(),
                    "#" + mr.getExternalId()));
        }

        if (mr.getMergedAt() != null && DateTimeUtils.isNight(mr.getMergedAt())
                && !DateTimeUtils.isNight(mr.getCreatedAt())) {
            flags.add(new ActivityFlag(
                    FlagType.NIGHT_WORK, Severity.INFO,
                    "PR #" + mr.getExternalId() + " zmergowany o " + mr.getMergedAt().toLocalTime(),
                    "#" + mr.getExternalId()));
        }

        return flags;
    }
}
