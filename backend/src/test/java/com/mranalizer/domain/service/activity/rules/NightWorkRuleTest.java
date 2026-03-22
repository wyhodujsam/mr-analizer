package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NightWorkRuleTest {

    private final NightWorkRule rule = new NightWorkRule();

    @Test
    void shouldFlagAt23() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 23, 30));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.NIGHT_WORK, flags.get(0).type());
    }

    @Test
    void shouldFlagAt3AM() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 3, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
    }

    @Test
    void shouldFlagAt22() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 22, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
    }

    @Test
    void shouldNotFlagAt6AM() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 6, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagAt8AM() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 8, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldNotFlagAt21() {
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 21, 59));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    private MergeRequest mrCreatedAt(LocalDateTime createdAt) {
        return MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(10, 5, 1))
                .createdAt(createdAt)
                .build();
    }
}
