package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoReviewRuleTest {

    private final NoReviewRule rule = new NoReviewRule();

    @Test
    void shouldFlagMergedPrWithNoReviews() {
        MergeRequest mr = mergedMr();
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.NO_REVIEW, flags.get(0).type());
    }

    @Test
    void shouldNotFlagMergedPrWithReviews() {
        MergeRequest mr = mergedMr();
        List<ReviewInfo> reviews = List.of(
                new ReviewInfo("reviewer1", "APPROVED", LocalDateTime.now()));
        List<ActivityFlag> flags = rule.evaluate(mr, reviews);
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagOpenPr() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1")
                .state("open")
                .diffStats(new DiffStats(10, 5, 1))
                .build();
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    private MergeRequest mergedMr() {
        return MergeRequest.builder()
                .externalId("1")
                .state("merged")
                .diffStats(new DiffStats(10, 5, 1))
                .build();
    }
}
