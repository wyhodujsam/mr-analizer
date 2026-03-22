package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeekendWorkRuleTest {

    private final WeekendWorkRule rule = new WeekendWorkRule();

    @Test
    void shouldFlagSaturday() {
        // 2026-03-07 is Saturday
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 7, 14, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.WEEKEND_WORK, flags.get(0).type());
        assertTrue(flags.get(0).description().contains("sobotę"));
    }

    @Test
    void shouldFlagSunday() {
        // 2026-03-08 is Sunday
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 8, 10, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
        assertEquals(FlagType.WEEKEND_WORK, flags.get(0).type());
    }

    @Test
    void shouldNotFlagMonday() {
        // 2026-03-09 is Monday
        MergeRequest mr = mrCreatedAt(LocalDateTime.of(2026, 3, 9, 10, 0));
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldFlagWeekendMerge() {
        // Created on Friday, merged on Saturday
        MergeRequest mr = MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(10, 5, 1))
                .createdAt(LocalDateTime.of(2026, 3, 6, 17, 0)) // Friday
                .mergedAt(LocalDateTime.of(2026, 3, 7, 9, 0))   // Saturday
                .build();
        List<ActivityFlag> flags = rule.evaluate(mr, List.of());
        assertEquals(1, flags.size());
    }

    private MergeRequest mrCreatedAt(LocalDateTime createdAt) {
        return MergeRequest.builder()
                .externalId("1")
                .diffStats(new DiffStats(10, 5, 1))
                .createdAt(createdAt)
                .build();
    }
}
