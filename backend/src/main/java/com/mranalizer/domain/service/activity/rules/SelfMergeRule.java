package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.util.List;

public class SelfMergeRule implements ActivityRule {

    @Override
    public List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews) {
        if (!"merged".equals(mr.getState()) || reviews.isEmpty()) {
            return List.of();
        }

        // Check if there are any APPROVED reviews
        List<ReviewInfo> approvals = reviews.stream()
                .filter(r -> "APPROVED".equals(r.state()))
                .toList();

        // No approvals at all — not a self-merge issue (NoReviewRule handles "no review")
        if (approvals.isEmpty()) {
            return List.of();
        }

        // Only flag if ALL approvals are from the author (self-approve)
        boolean allApprovalsFromAuthor = approvals.stream()
                .allMatch(r -> r.reviewer().equals(mr.getAuthor()));

        if (allApprovalsFromAuthor) {
            return List.of(new ActivityFlag(
                    FlagType.SELF_MERGE, Severity.WARNING,
                    "PR #" + mr.getExternalId() + " zatwierdzony tylko przez autora (" + mr.getAuthor() + ")",
                    "#" + mr.getExternalId()));
        }

        return List.of();
    }
}
