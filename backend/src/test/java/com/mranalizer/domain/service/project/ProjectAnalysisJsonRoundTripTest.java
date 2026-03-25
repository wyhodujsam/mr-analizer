package com.mranalizer.domain.service.project;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mranalizer.domain.model.RuleResult;
import com.mranalizer.domain.model.Verdict;
import com.mranalizer.domain.model.project.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalysisJsonRoundTripTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void prAnalysisRowRoundTrip() throws Exception {
        PrAnalysisRow row = new PrAnalysisRow(
                "42", "Fix login", "alice", "merged", "https://github.com/test/42",
                LocalDateTime.of(2026, 3, 10, 10, 0), LocalDateTime.of(2026, 3, 10, 12, 0),
                200, 50,
                0.75, Verdict.AUTOMATABLE,
                List.of(new RuleResult("boost:hasTests", true, 0.15, "PR contains tests")),
                "LLM says good",
                true, true,
                List.of("src/test/features/login.feature"),
                List.of("specs/001/spec.md")
        );

        String json = mapper.writeValueAsString(row);
        PrAnalysisRow deserialized = mapper.readValue(json, PrAnalysisRow.class);

        assertEquals(row.prId(), deserialized.prId());
        assertEquals(row.aiScore(), deserialized.aiScore());
        assertEquals(row.aiVerdict(), deserialized.aiVerdict());
        assertEquals(row.ruleResults().size(), deserialized.ruleResults().size());
        assertEquals(row.ruleResults().get(0).ruleName(), deserialized.ruleResults().get(0).ruleName());
        assertEquals(row.hasBdd(), deserialized.hasBdd());
        assertEquals(row.bddFiles(), deserialized.bddFiles());
        assertEquals(row.createdAt(), deserialized.createdAt());
    }

    @Test
    void projectSummaryRoundTrip() throws Exception {
        ProjectSummary summary = new ProjectSummary(
                50, 30, 12, 8, 60.0, 24.0, 16.0, 0.62,
                List.of(new RuleFrequency("boost:hasTests", 35, 0.15)),
                List.of(new ScoreHistogramBucket(0.0, 0.2, 5)),
                18, 36.0, 10, 20.0
        );

        String json = mapper.writeValueAsString(summary);
        ProjectSummary deserialized = mapper.readValue(json, ProjectSummary.class);

        assertEquals(summary.totalPrs(), deserialized.totalPrs());
        assertEquals(summary.avgScore(), deserialized.avgScore());
        assertEquals(summary.topRules().size(), deserialized.topRules().size());
        assertEquals(summary.histogram().size(), deserialized.histogram().size());
    }

    @Test
    void rowsListRoundTrip() throws Exception {
        List<PrAnalysisRow> rows = List.of(
                new PrAnalysisRow("1", "PR 1", "alice", "merged", null,
                        LocalDateTime.of(2026, 3, 10, 10, 0), LocalDateTime.of(2026, 3, 10, 12, 0),
                        100, 20, 0.8, Verdict.AUTOMATABLE, List.of(), null,
                        true, false, List.of("test.feature"), List.of()),
                new PrAnalysisRow("2", "PR 2", "bob", "merged", null,
                        LocalDateTime.of(2026, 3, 11, 10, 0), null,
                        50, 10, 0.3, Verdict.NOT_SUITABLE, List.of(), null,
                        false, true, List.of(), List.of("spec.md"))
        );

        String json = mapper.writeValueAsString(rows);
        List<PrAnalysisRow> deserialized = mapper.readValue(json, new TypeReference<>() {});

        assertEquals(2, deserialized.size());
        assertEquals(Verdict.AUTOMATABLE, deserialized.get(0).aiVerdict());
        assertEquals(Verdict.NOT_SUITABLE, deserialized.get(1).aiVerdict());
    }
}
