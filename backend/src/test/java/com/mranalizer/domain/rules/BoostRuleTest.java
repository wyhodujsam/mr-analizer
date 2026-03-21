package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BoostRule — will not compile until BoostRule is implemented (T018).
 */
class BoostRuleTest {

    @Nested
    @DisplayName("Description/title keyword boost")
    class DescriptionKeywordBoost {

        @Test
        @DisplayName("Title containing 'refactor' should match with positive weight")
        void titleWithRefactor_shouldMatch() {
            MergeRequest mr = baseMrBuilder()
                    .title("refactor: extract service layer")
                    .build();

            BoostRule rule = BoostRule.byTitleKeywords(List.of("refactor", "cleanup", "simplify"), 0.1);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() > 0, "Boost weight should be positive");
        }
    }

    @Nested
    @DisplayName("Has tests boost")
    class HasTestsBoost {

        @Test
        @DisplayName("PR with test files should match")
        void prWithTests_shouldMatch() {
            MergeRequest mr = baseMrBuilder()
                    .hasTests(true)
                    .build();

            BoostRule rule = BoostRule.byHasTests(0.1);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() > 0);
        }
    }

    @Nested
    @DisplayName("Changed files range boost")
    class ChangedFilesRangeBoost {

        @Test
        @DisplayName("5 files in 3-15 range should match")
        void fiveFilesInRange_shouldMatch() {
            List<ChangedFile> files = IntStream.rangeClosed(1, 5)
                    .mapToObj(i -> new ChangedFile("src/File" + i + ".java", 10, 5, "modified"))
                    .toList();
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(50, 25, 5))
                    .build();

            BoostRule rule = BoostRule.byChangedFilesRange(3, 15, 0.05);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() > 0);
        }

        @Test
        @DisplayName("20 files outside 3-15 range should not match")
        void twentyFilesOutOfRange_shouldNotMatch() {
            List<ChangedFile> files = IntStream.rangeClosed(1, 20)
                    .mapToObj(i -> new ChangedFile("src/File" + i + ".java", 5, 2, "modified"))
                    .toList();
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(100, 40, 20))
                    .build();

            BoostRule rule = BoostRule.byChangedFilesRange(3, 15, 0.05);
            RuleResult result = rule.evaluate(mr);

            assertFalse(result.matched());
        }
    }

    @Nested
    @DisplayName("Label boost")
    class LabelBoost {

        @Test
        @DisplayName("PR with 'tech-debt' label should match")
        void prWithTechDebtLabel_shouldMatch() {
            MergeRequest mr = baseMrBuilder()
                    .labels(List.of("tech-debt", "backend"))
                    .build();

            BoostRule rule = BoostRule.byLabels(List.of("tech-debt", "refactor"), 0.05);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() > 0);
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("byHasTests with minimal MR should not throw")
        void byHasTests_withMinimalMr_doesNotThrow() {
            var rule = BoostRule.byHasTests(0.1);
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertFalse(result.matched());
        }

        @Test
        @DisplayName("byChangedFilesRange with null diffStats should not throw")
        void byChangedFilesRange_withNullDiffStats_doesNotThrow() {
            var rule = BoostRule.byChangedFilesRange(3, 15, 0.05);
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertFalse(result.matched());
        }

        @Test
        @DisplayName("byLabels with null labels should not throw")
        void byLabels_withNullLabels_doesNotThrow() {
            var rule = BoostRule.byLabels(List.of("tech-debt"), 0.05);
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
