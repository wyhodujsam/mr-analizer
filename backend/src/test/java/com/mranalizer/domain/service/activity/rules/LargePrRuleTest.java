package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LargePrRuleTest {

    private final LargePrRule rule = new LargePrRule();

    @Test
    void shouldFlagCriticalWhenOver1000Lines() {
        MergeRequest mr = mrWithLines(800, 300); // 1100
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.VERY_LARGE_PR, flags.get(0).type());
        assertEquals(Severity.CRITICAL, flags.get(0).severity());
    }

    @Test
    void shouldFlagWarningWhenOver500Lines() {
        MergeRequest mr = mrWithLines(400, 200); // 600
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.LARGE_PR, flags.get(0).type());
        assertEquals(Severity.WARNING, flags.get(0).severity());
    }

    @Test
    void shouldNotFlagWhenUnder500Lines() {
        MergeRequest mr = mrWithLines(200, 100); // 300
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagExactly500Lines() {
        MergeRequest mr = mrWithLines(300, 200); // 500
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldFlagCriticalAtExactly1001Lines() {
        MergeRequest mr = mrWithLines(501, 500); // 1001
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(FlagType.VERY_LARGE_PR, flags.get(0).type());
    }

    private MergeRequest mrWithLines(int additions, int deletions) {
        return MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(additions, deletions, 5))
                .build();
    }
}
