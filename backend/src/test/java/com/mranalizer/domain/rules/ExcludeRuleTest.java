package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;
import com.mranalizer.domain.scoring.ScoringEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExcludeRule — will not compile until ExcludeRule is implemented (T018).
 */
class ExcludeRuleTest {

    @Nested
    @DisplayName("Label exclusion")
    class LabelExclusion {

        @Test
        @DisplayName("PR with 'hotfix' label should be excluded")
        void prWithHotfixLabel_shouldBeExcluded() {
            MergeRequest mr = baseMrBuilder()
                    .labels(List.of("hotfix"))
                    .build();

            ExcludeRule rule = ExcludeRule.byLabels(List.of("hotfix", "emergency"));
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() <= -999.0, "Exclude weight should be sentinel value");
            assertTrue(result.reason().contains("excluded by label"));
        }

        @Test
        @DisplayName("PR without excluded labels should not be excluded")
        void prWithoutExcludedLabels_shouldNotBeExcluded() {
            MergeRequest mr = baseMrBuilder()
                    .labels(List.of("feature", "backend"))
                    .build();

            ExcludeRule rule = ExcludeRule.byLabels(List.of("hotfix", "emergency"));
            RuleResult result = rule.evaluate(mr);

            assertFalse(result.matched());
        }
    }

    @Nested
    @DisplayName("Minimum changed files")
    class MinChangedFiles {

        @Test
        @DisplayName("PR with 1 file should be excluded when min is 2")
        void prWithOneFile_shouldBeExcluded() {
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(List.of(new ChangedFile("src/A.java", 10, 5, "modified")))
                    .diffStats(new DiffStats(10, 5, 1))
                    .build();

            ExcludeRule rule = ExcludeRule.byMinChangedFiles(2);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() <= -999.0);
        }

        @Test
        @DisplayName("PR with 3 files should not be excluded when min is 2")
        void prWithThreeFiles_shouldNotBeExcluded() {
            List<ChangedFile> files = List.of(
                    new ChangedFile("src/A.java", 10, 5, "modified"),
                    new ChangedFile("src/B.java", 8, 3, "modified"),
                    new ChangedFile("src/C.java", 6, 2, "added")
            );
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(24, 10, 3))
                    .build();

            ExcludeRule rule = ExcludeRule.byMinChangedFiles(2);
            RuleResult result = rule.evaluate(mr);

            assertFalse(result.matched());
        }
    }

    @Nested
    @DisplayName("Maximum changed files")
    class MaxChangedFiles {

        @Test
        @DisplayName("PR with 51 files should be excluded when max is 50")
        void prWith51Files_shouldBeExcluded() {
            List<ChangedFile> files = IntStream.rangeClosed(1, 51)
                    .mapToObj(i -> new ChangedFile("src/File" + i + ".java", 5, 2, "modified"))
                    .toList();
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(255, 102, 51))
                    .build();

            ExcludeRule rule = ExcludeRule.byMaxChangedFiles(50);
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() <= -999.0);
        }
    }

    @Nested
    @DisplayName("File extensions only")
    class FileExtensionsOnly {

        @Test
        @DisplayName("PR with only .yml files should be excluded")
        void prWithOnlyYmlFiles_shouldBeExcluded() {
            List<ChangedFile> files = List.of(
                    new ChangedFile("config/app.yml", 5, 2, "modified"),
                    new ChangedFile("deploy/k8s.yml", 10, 3, "modified")
            );
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(15, 5, 2))
                    .build();

            ExcludeRule rule = ExcludeRule.byFileExtensionsOnly(List.of(".yml", ".yaml", ".toml", ".properties"));
            RuleResult result = rule.evaluate(mr);

            assertTrue(result.matched());
            assertTrue(result.weight() <= -999.0);
        }

        @Test
        @DisplayName("PR with .java files should not be excluded")
        void prWithJavaFiles_shouldNotBeExcluded() {
            List<ChangedFile> files = List.of(
                    new ChangedFile("src/Service.java", 20, 10, "modified"),
                    new ChangedFile("config/app.yml", 5, 2, "modified")
            );
            MergeRequest mr = baseMrBuilder()
                    .changedFiles(files)
                    .diffStats(new DiffStats(25, 12, 2))
                    .build();

            ExcludeRule rule = ExcludeRule.byFileExtensionsOnly(List.of(".yml", ".yaml", ".toml", ".properties"));
            RuleResult result = rule.evaluate(mr);

            assertFalse(result.matched());
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("byLabels with null labels should not throw")
        void byLabels_withNullLabels_doesNotThrow() {
            var rule = ExcludeRule.byLabels(List.of("hotfix"));
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertFalse(result.matched());
        }

        @Test
        @DisplayName("byMinChangedFiles with null diffStats should not throw")
        void byMinChangedFiles_withNullDiffStats_doesNotThrow() {
            var rule = ExcludeRule.byMinChangedFiles(2);
            var mr = MergeRequest.builder().externalId("1").title("test").build();
            var result = rule.evaluate(mr);
            assertTrue(result.matched());
        }

        @Test
        @DisplayName("byFileExtensionsOnly with null changedFiles should not throw")
        void byFileExtensionsOnly_withNullChangedFiles_doesNotThrow() {
            var rule = ExcludeRule.byFileExtensionsOnly(List.of(".yml"));
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
