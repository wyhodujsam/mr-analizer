package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.util.ArrayList;
import java.util.List;

public class LargePrRule implements ActivityRule {

    private static final int WARNING_THRESHOLD = 500;
    private static final int CRITICAL_THRESHOLD = 1000;

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        int totalLines = mr.getDiffStats().additions() + mr.getDiffStats().deletions();
        List<ActivityFlag> flags = new ArrayList<>();

        if (totalLines > CRITICAL_THRESHOLD) {
            flags.add(new ActivityFlag(
                    FlagType.VERY_LARGE_PR, Severity.CRITICAL,
                    "PR #" + mr.getExternalId() + " ma " + totalLines + " linii zmian (próg: " + CRITICAL_THRESHOLD + ")",
                    "#" + mr.getExternalId()));
        } else if (totalLines > WARNING_THRESHOLD) {
            flags.add(new ActivityFlag(
                    FlagType.LARGE_PR, Severity.WARNING,
                    "PR #" + mr.getExternalId() + " ma " + totalLines + " linii zmian (próg: " + WARNING_THRESHOLD + ")",
                    "#" + mr.getExternalId()));
        }

        return flags;
    }
}
