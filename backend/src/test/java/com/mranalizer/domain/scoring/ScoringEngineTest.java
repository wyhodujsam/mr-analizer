package com.mranalizer.domain.scoring;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.rules.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {

    private ScoringEngine engine;
    private static final LlmAssessment NO_LLM = new LlmAssessment(0.0, "", "none");

    @BeforeEach
    void setUp() {
        engine = new ScoringEngine(new ScoringConfig()); // base=0.5, automatable=0.7, maybe=0.4
    }

    @Nested
    @DisplayName("Base score behavior")
    class BaseScore {

        @Test
        @DisplayName("No rules matched should give base score 0.5 and MAYBE verdict")
        void noRulesMatched_shouldGiveBaseScore() {
            Rule nonMatching = testRule("no-match", false, 0.0, "not matched");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(nonMatching), NO_LLM);

            assertEquals(0.5, result.getScore(), 0.001);
            assertEquals(Verdict.MAYBE, result.getVerdict());
        }
    }

    @Nested
    @DisplayName("Boost behavior")
    class Boosts {

        @Test
        @DisplayName("Boosts should increase score correctly")
        void boosts_shouldIncreaseScore() {
            Rule boost1 = testRule("boost-1", true, 0.1, "boost reason 1");
            Rule boost2 = testRule("boost-2", true, 0.15, "boost reason 2");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(boost1, boost2), NO_LLM);

            assertEquals(0.75, result.getScore(), 0.001);
            assertEquals(Verdict.AUTOMATABLE, result.getVerdict());
            assertEquals(2, result.getMatchedRules().size());
        }
    }

    @Nested
    @DisplayName("Penalty behavior")
    class Penalties {

        @Test
        @DisplayName("Penalties should decrease score correctly")
        void penalties_shouldDecreaseScore() {
            Rule penalty1 = testRule("penalty-1", true, -0.1, "penalty reason 1");
            Rule penalty2 = testRule("penalty-2", true, -0.05, "penalty reason 2");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(penalty1, penalty2), NO_LLM);

            assertEquals(0.35, result.getScore(), 0.001);
            assertEquals(Verdict.NOT_SUITABLE, result.getVerdict());
        }
    }

    @Nested
    @DisplayName("Exclusion behavior")
    class Exclusion {

        @Test
        @DisplayName("Exclude rule should override everything with score=0 and NOT_SUITABLE")
        void excludeRule_shouldOverrideEverything() {
            Rule boost = testRule("boost", true, 0.2, "boost");
            Rule exclude = testRule("exclude", true, ScoringEngine.EXCLUDE_WEIGHT, "excluded by label: hotfix");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(boost, exclude), NO_LLM);

            assertEquals(0.0, result.getScore(), 0.001);
            assertEquals(Verdict.NOT_SUITABLE, result.getVerdict());
            assertTrue(result.getReasons().stream().anyMatch(r -> r.contains("excluded by label")));
        }
    }

    @Nested
    @DisplayName("Score clamping")
    class ScoreClamping {

        @Test
        @DisplayName("Score should not exceed 1.0")
        void score_shouldNotExceed1() {
            Rule bigBoost = testRule("big-boost", true, 0.8, "huge boost");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(bigBoost), NO_LLM);

            assertEquals(1.0, result.getScore(), 0.001);
        }

        @Test
        @DisplayName("Score should not go below 0.0")
        void score_shouldNotGoBelow0() {
            Rule bigPenalty = testRule("big-penalty", true, -0.8, "huge penalty");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(bigPenalty), NO_LLM);

            assertEquals(0.0, result.getScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("Verdict threshold boundaries")
    class VerdictThresholds {

        @Test
        @DisplayName("Score 0.39 should give NOT_SUITABLE")
        void score039_shouldBeNotSuitable() {
            // base 0.5, penalty -0.11 = 0.39
            Rule penalty = testRule("penalty", true, -0.11, "penalty");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(penalty), NO_LLM);

            assertEquals(0.39, result.getScore(), 0.001);
            assertEquals(Verdict.NOT_SUITABLE, result.getVerdict());
        }

        @Test
        @DisplayName("Score 0.4 should give MAYBE")
        void score040_shouldBeMaybe() {
            // base 0.5, penalty -0.1 = 0.4
            Rule penalty = testRule("penalty", true, -0.1, "penalty");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(penalty), NO_LLM);

            assertEquals(0.4, result.getScore(), 0.001);
            assertEquals(Verdict.MAYBE, result.getVerdict());
        }

        @Test
        @DisplayName("Score 0.69 should give MAYBE")
        void score069_shouldBeMaybe() {
            // base 0.5, boost 0.19 = 0.69
            Rule boost = testRule("boost", true, 0.19, "boost");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(boost), NO_LLM);

            assertEquals(0.69, result.getScore(), 0.001);
            assertEquals(Verdict.MAYBE, result.getVerdict());
        }

        @Test
        @DisplayName("Score 0.7 should give AUTOMATABLE")
        void score070_shouldBeAutomatable() {
            // base 0.5, boost 0.2 = 0.7
            Rule boost = testRule("boost", true, 0.2, "boost");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(boost), NO_LLM);

            assertEquals(0.7, result.getScore(), 0.001);
            assertEquals(Verdict.AUTOMATABLE, result.getVerdict());
        }
    }

    @Nested
    @DisplayName("LLM adjustment")
    class LlmAdjustment {

        @Test
        @DisplayName("Positive LLM adjustment should increase score")
        void positiveLlmAdjustment_shouldIncreaseScore() {
            LlmAssessment llm = new LlmAssessment(0.1, "looks good for automation", "openai");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(), llm);

            assertEquals(0.6, result.getScore(), 0.001);
            assertTrue(result.getReasons().stream().anyMatch(r -> r.contains("llm")));
        }

        @Test
        @DisplayName("Negative LLM adjustment should decrease score")
        void negativeLlmAdjustment_shouldDecreaseScore() {
            LlmAssessment llm = new LlmAssessment(-0.15, "complex logic", "openai");

            AnalysisResult result = engine.evaluate(buildMr(), List.of(), llm);

            assertEquals(0.35, result.getScore(), 0.001);
        }
    }

    // --- Helper methods ---

    private Rule testRule(String name, boolean matched, double weight, String reason) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                return new RuleResult(name, matched, weight, reason);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    private MergeRequest buildMr() {
        return MergeRequest.builder()
                .externalId("test-1")
                .title("test merge request")
                .description("test description")
                .author("tester")
                .sourceBranch("feature/test")
                .targetBranch("main")
                .state("merged")
                .labels(List.of())
                .changedFiles(List.of(new ChangedFile("src/Main.java", 10, 5, "modified")))
                .diffStats(new DiffStats(10, 5, 1))
                .hasTests(false)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(0)
                .provider("gitlab")
                .url("https://example.com/mr/1")
                .projectSlug("test/project")
                .build();
    }
}
