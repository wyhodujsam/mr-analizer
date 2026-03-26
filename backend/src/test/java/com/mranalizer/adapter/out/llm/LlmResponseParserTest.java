package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.LlmAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmResponseParserTest {

    private LlmResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new LlmResponseParser(new ObjectMapper());
    }

    @Test
    void happyPath_fullJson() {
        String json = """
            {
              "scoreAdjustment": 0.15,
              "comment": "Good PR for automation",
              "overallAutomatability": 75,
              "categories": [{"name": "code-change", "score": 80, "reasoning": "Simple refactor"}],
              "humanOversightRequired": [{"area": "security", "reasoning": "Touches auth"}],
              "whyLlmFriendly": ["repetitive", "well-defined"],
              "summaryTable": [{"aspect": "Complexity", "score": 3, "note": "Low"}]
            }
            """;

        LlmAssessment result = parser.parseJsonResponse(json, "test");

        assertEquals(0.15, result.scoreAdjustment(), 0.01);
        assertEquals("Good PR for automation", result.comment());
        assertEquals(75, result.overallAutomatability());
        assertEquals(1, result.categories().size());
        assertEquals("code-change", result.categories().get(0).name());
        assertEquals(1, result.humanOversightRequired().size());
        assertEquals(2, result.whyLlmFriendly().size());
        assertEquals(1, result.summaryTable().size());
        assertEquals("test", result.provider());
    }

    @Test
    void minimalJson_scoreAndCommentOnly() {
        String json = """
            {"scoreAdjustment": 0.1, "comment": "ok"}
            """;

        LlmAssessment result = parser.parseJsonResponse(json, "test");

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertEquals("ok", result.comment());
        assertTrue(result.categories().isEmpty());
        assertTrue(result.humanOversightRequired().isEmpty());
    }

    @Test
    void jsonWrappedInMarkdown() {
        String text = """
            Here is my analysis:
            ```json
            {"scoreAdjustment": 0.2, "comment": "looks good"}
            ```
            That's my assessment.
            """;

        LlmAssessment result = parser.parseJsonResponse(text, "test");

        assertEquals(0.2, result.scoreAdjustment(), 0.01);
        assertEquals("looks good", result.comment());
    }

    @Test
    void noJsonInText() {
        LlmAssessment result = parser.parseJsonResponse("No JSON here at all", "test");

        assertEquals(0.0, result.scoreAdjustment());
        assertTrue(result.comment().contains("no JSON"));
    }

    @Test
    void scoreClamping_above() {
        String json = """
            {"scoreAdjustment": 0.9, "comment": "too high"}
            """;

        LlmAssessment result = parser.parseJsonResponse(json, "test");
        assertEquals(0.5, result.scoreAdjustment(), 0.01);
    }

    @Test
    void scoreClamping_below() {
        String json = """
            {"scoreAdjustment": -0.8, "comment": "too low"}
            """;

        LlmAssessment result = parser.parseJsonResponse(json, "test");
        assertEquals(-0.5, result.scoreAdjustment(), 0.01);
    }

    @Test
    void emptyString() {
        LlmAssessment result = parser.parseJsonResponse("", "test");
        assertEquals(0.0, result.scoreAdjustment());
    }
}
