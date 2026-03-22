package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.model.activity.FlagType;
import com.mranalizer.domain.model.activity.Severity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AggregateRulesTest {

    private final AggregateRules rules = new AggregateRules();

    @Test
    void shouldFlagHighWeekendRatio() {
        // 4 out of 10 = 40% > 30%
        List<MergeRequest> prs = new ArrayList<>();
        // 4 weekend PRs (Saturday 2026-03-07)
        for (int i = 0; i < 4; i++) {
            prs.add(mrCreatedAt(LocalDateTime.of(2026, 3, 7, 10 + i, 0)));
        }
        // 6 weekday PRs (Monday 2026-03-09)
        for (int i = 0; i < 6; i++) {
            prs.add(mrCreatedAt(LocalDateTime.of(2026, 3, 9, 10 + i, 0)));
        }

        List<ActivityFlag> flags = rules.evaluate(prs);
        assertEquals(1, flags.size());
        assertEquals(FlagType.HIGH_WEEKEND_RATIO, flags.get(0).type());
        assertEquals(Severity.WARNING, flags.get(0).severity());
    }

    @Test
    void shouldNotFlagLowWeekendRatio() {
        // 2 out of 10 = 20% < 30%
        List<MergeRequest> prs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            prs.add(mrCreatedAt(LocalDateTime.of(2026, 3, 7, 10 + i, 0)));
        }
        for (int i = 0; i < 8; i++) {
            prs.add(mrCreatedAt(LocalDateTime.of(2026, 3, 9, 10 + i, 0)));
        }

        List<ActivityFlag> flags = rules.evaluate(prs);
        assertTrue(flags.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNoPrs() {
        List<ActivityFlag> flags = rules.evaluate(List.of());
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
