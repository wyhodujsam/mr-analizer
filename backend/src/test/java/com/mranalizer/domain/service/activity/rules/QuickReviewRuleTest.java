package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuickReviewRuleTest {

    private final QuickReviewRule rule = new QuickReviewRule();

    @Test
    void shouldFlagCriticalWhenLargePrMergedIn3Minutes() {
        MergeRequest mr = mrWithSizeAndTime(150, 3);
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.SUSPICIOUS_QUICK_MERGE, flags.get(0).type());
        assertEquals(Severity.CRITICAL, flags.get(0).severity());
    }

    @Test
    void shouldFlagWarningWhenMediumPrMergedIn8Minutes() {
        MergeRequest mr = mrWithSizeAndTime(80, 8);
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.QUICK_REVIEW, flags.get(0).type());
        assertEquals(Severity.WARNING, flags.get(0).severity());
    }

    @Test
    void shouldNotFlagSmallPrMergedQuickly() {
        MergeRequest mr = mrWithSizeAndTime(30, 2);
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagLargePrMergedSlowly() {
        MergeRequest mr = mrWithSizeAndTime(500, 60);
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagWhenNotMerged() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(200, 0, 5))
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .build();
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    private MergeRequest mrWithSizeAndTime(int totalLines, int minutes) {
        LocalDateTime created = LocalDateTime.of(2026, 3, 1, 10, 0);
        return MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(totalLines, 0, 5))
                .createdAt(created)
                .mergedAt(created.plusMinutes(minutes))
                .build();
    }
}
