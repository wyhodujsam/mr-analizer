package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PenalizeRule — will not compile until PenalizeRule is implemented (T018).
 */
class PenalizeRuleTest {

    @Nested
    @DisplayName("Large diff penalty")
    class LargeDiffPenalty {

        @Test
        @DisplayName("600 lines of diff should be penalized")
        void largeDiff_shouldBePenalized() {
            MergeRequest mr = baseMrBuilder()
                    .diffStats(new DiffStats(400, 200, 5))
                    .build();

            PenalizeRule rule = PenalizeRule.byLargeDiff(500, -0.15);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() < 0, "Penalty weight should be negative");
        }

        @Test
        @DisplayName("200 lines of diff should not be penalized")
        void smallDiff_shouldNotBePenalized() {
            MergeRequest mr = baseMrBuilder()
                    .diffStats(new DiffStats(120, 80, 3))
                    .build();

            PenalizeRule rule = PenalizeRule.byLargeDiff(500, -0.15);
            RuleResult result = rule.evaluate(mr);

            assertFalse(result.matched());
        }
    }

    @Nested
    @DisplayName("No description penalty")
    class NoDescriptionPenalty {

        @Test
        @DisplayName("Null description should be penalized")
        void nullDescription_shouldBePenalized() {
            MergeRequest mr = baseMrBuilder()
                    .description(null)
                    .build();

            PenalizeRule rule = PenalizeRule.byNoDescription(-0.1);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() < 0);
        }

        @Test
        @DisplayName("Blank description should be penalized")
        void blankDescription_shouldBePenalized() {
            MergeRequest mr = baseMrBuilder()
                    .description("   ")
                    .build();

            PenalizeRule rule = PenalizeRule.byNoDescription(-0.1);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() < 0);
        }
    }

    @Nested
    @DisplayName("Touches config penalty")
    class TouchesConfigPenalty {

        @Test
        @DisplayName("Changes including .yml file should be penalized")
        void ymlFileChanged_shouldBePenalized() {
            List<ChangedFile> files = List.of(
                    new ChangedFile("src/Service.java", 20, 10, "modified"),
                    new ChangedFile("config/application.yml", 5, 2, "modified")
            );
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(25, 12, 2))
                    .build();

            PenalizeRule rule = PenalizeRule.byTouchesConfig(
                    List.of(".yml", ".yaml", ".toml", ".properties"), -0.05);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() < 0);
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("byLargeDiff with null diffStats should not throw")
        void byLargeDiff_withNullDiffStats_doesNotThrow() {
            var rule = PenalizeRule.byLargeDiff(500, -0.15);
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertFalse(result.matched());
        }

        @Test
        @DisplayName("byTouchesConfig with null changedFiles should not throw")
        void byTouchesConfig_withNullChangedFiles_doesNotThrow() {
            var rule = PenalizeRule.byTouchesConfig(List.of(".yml"), -0.05);
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertFalse(result.matched());
        }
    }

    private MergeRequest.Builder baseMrBuilder() {
        return MergeRequest.builder()
                .externalId("test-1")
                .title("test merge request")
                .description("test description")
                .author("tester")
                .sourceBranch("feature/test")
                .targetBranch("main")
                .state("merged")
                .labels(List.of())
                .changedFiles(List.of())
                .diffStats(new DiffStats(0, 0, 0))
                .hasTests(false)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(0)
                .provider("gitlab")
                .url("https://example.com/mr/1")
                .projectSlug("test/project");
    }
}
