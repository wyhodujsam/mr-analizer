package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.util.List;

public class NoReviewRule implements ActivityRule {

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        if (!"merged".equals(mr.getState())) {
            return List.of();
        }

        if (reviews.isEmpty()) {
            return List.of(new ActivityFlag(
                    FlagType.NO_REVIEW, Severity.WARNING,
                    "PR #" + mr.getExternalId() + " zmergowany bez żadnego review",
                    "#" + mr.getExternalId()));
        }

        return List.of();
    }
}
