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

class SelfMergeRuleTest {

    private final SelfMergeRule rule = new SelfMergeRule();

    @Test
    void shouldFlagSelfApproval() {
        MergeRequest mr = mergedMr("jan.kowalski");
        List<ReviewInfo> reviews = List.of(
                new ReviewInfo("jan.kowalski", "APPROVED", LocalDateTime.now()));
        List<ActivityFlag> flags = rule.evaluate(mr, reviews);
        assertEquals(1, flags.size());
        assertEquals(FlagType.SELF_MERGE, flags.get(0).type());
    }

    @Test
    void shouldNotFlagExternalApproval() {
        MergeRequest mr = mergedMr("jan.kowalski");
        List<ReviewInfo> reviews = List.of(
                new ReviewInfo("anna.nowak", "APPROVED", LocalDateTime.now()));
        List<ActivityFlag> flags = rule.evaluate(mr, reviews);
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagOpenPr() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1")
                .author("jan.kowalski")
                .state("open")
                .diffStats(new DiffStats(10, 5, 1))
                .build();
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagWhenNoReviews() {
        // NoReviewRule handles the "no reviews" case
        MergeRequest mr = mergedMr("jan.kowalski");
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    private MergeRequest mergedMr(String author) {
        return MergeRequest.builder()
                .externalId("1")
                .author(author)
                .state("merged")
                .diffStats(new DiffStats(10, 5, 1))
                .build();
    }
}
