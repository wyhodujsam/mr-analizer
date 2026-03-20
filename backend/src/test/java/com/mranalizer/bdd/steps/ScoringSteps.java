package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScoringSteps {

    private MergeRequest.Builder mrBuilder;
    private List<ChangedFile> changedFiles;
    private ScoringEngine scoringEngine;
    private AnalysisResult result;
    private static final LlmAssessment NO_LLM = new LlmAssessment(0.0, "", "none");

    @Before
    public void setUp() {
        mrBuilder = MergeRequest.builder()
                .externalId("test-1")
                .title("default title")
                .description("default description")
                .author("tester")
                .sourceBranch("feature/test")
                .targetBranch("main")
                .state("merged")
                .labels(List.of())
                .hasTests(false)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(0)
                .provider("gitlab")
                .url("https://example.com/mr/1")
                .projectSlug("test/project");
        changedFiles = new ArrayList<>();
    }

    @Given("the scoring engine is configured with default thresholds")
    public void scoringEngineWithDefaultThresholds() {
        scoringEngine = new ScoringEngine(new ScoringConfig());
    }

    @Given("a merge request with label {string}")
    public void mrWithLabel(String label) {
        mrBuilder.labels(List.of(label));
    }

    @Given("a merge request with title {string}")
    public void mrWithTitle(String title) {
        mrBuilder.title(title);
    }

    @And("the merge request has {int} changed files")
    public void mrHasChangedFiles(int count) {
        changedFiles.clear();
        for (int i = 0; i < count; i++) {
            changedFiles.add(new ChangedFile("src/File" + i + ".java", 10, 5, "modified"));
        }
        mrBuilder.changedFiles(changedFiles);
        mrBuilder.diffStats(new DiffStats(count * 10, count * 5, count));
    }

    @And("the merge request has tests")
    public void mrHasTests() {
        mrBuilder.hasTests(true);
    }

    @Given("a merge request with {int} changed file")
    public void mrWithSingleFile(int count) {
        changedFiles.clear();
        for (int i = 0; i < count; i++) {
            changedFiles.add(new ChangedFile("src/File" + i + ".java", 10, 5, "modified"));
        }
        mrBuilder.changedFiles(changedFiles);
        mrBuilder.diffStats(new DiffStats(count * 10, count * 5, count));
    }

    @And("the merge request has {int} lines of diff")
    public void mrHasDiffLines(int lines) {
        mrWithDiffLines(lines);
    }

    @Given("a merge request with {int} lines of diff")
    public void mrWithDiffLines(int lines) {
        int additions = lines / 2;
        int deletions = lines - additions;
        if (changedFiles.isEmpty()) {
            changedFiles.add(new ChangedFile("src/BigFile.java", additions, deletions, "modified"));
        }
        mrBuilder.changedFiles(changedFiles);
        mrBuilder.diffStats(new DiffStats(additions, deletions, changedFiles.size()));
    }

    @And("the merge request has no description")
    public void mrHasNoDescription() {
        mrBuilder.description(null);
    }

    @Given("a merge request with only {string} and {string} files")
    public void mrWithOnlyExtensions(String ext1, String ext2) {
        changedFiles.clear();
        changedFiles.add(new ChangedFile("config/app" + ext1, 5, 2, "modified"));
        changedFiles.add(new ChangedFile("config/build" + ext2, 3, 1, "modified"));
        mrBuilder.changedFiles(changedFiles);
        mrBuilder.diffStats(new DiffStats(8, 3, 2));
    }

    @And("the merge request has label {string}")
    public void mrHasLabel(String label) {
        List<String> existing = new ArrayList<>(mrBuilder.build().getLabels() != null
                ? mrBuilder.build().getLabels() : List.of());
        existing.add(label);
        mrBuilder.labels(existing);
    }

    @And("the merge request has a description {string}")
    public void mrHasDescription(String description) {
        mrBuilder.description(description);
    }

    @And("the merge request touches config files")
    public void mrTouchesConfigFiles() {
        changedFiles.add(new ChangedFile("config/application.yml", 5, 2, "modified"));
        mrBuilder.changedFiles(changedFiles);
    }

    // --- When steps ---

    @When("the scoring engine evaluates it")
    public void scoringEngineEvaluates() {
        MergeRequest mr = buildMr();
        List<Rule> rules = createDefaultRules(mr);
        result = scoringEngine.evaluate(mr, rules, NO_LLM);
    }

    @When("the scoring engine evaluates it with a minimum {int} files rule")
    public void scoringEngineEvaluatesWithMinFilesRule(int minFiles) {
        MergeRequest mr = buildMr();
        List<Rule> rules = List.of(createMinFilesExcludeRule(minFiles));
        result = scoringEngine.evaluate(mr, rules, NO_LLM);
    }

    @When("the scoring engine evaluates it with a config-only exclusion rule")
    public void scoringEngineEvaluatesWithConfigOnlyRule() {
        MergeRequest mr = buildMr();
        List<String> configExtensions = List.of(".yml", ".toml", ".yaml", ".properties", ".xml", ".json");
        Rule configOnlyRule = new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mergeRequest) {
                boolean allConfig = mergeRequest.getChangedFiles() != null
                        && !mergeRequest.getChangedFiles().isEmpty()
                        && mergeRequest.getChangedFiles().stream()
                        .allMatch(f -> configExtensions.stream().anyMatch(ext -> f.path().endsWith(ext)));
                return new RuleResult("file-extensions-only", allConfig,
                        allConfig ? ScoringEngine.EXCLUDE_WEIGHT : 0.0,
                        allConfig ? "excluded: config files only" : "has code files");
            }

            @Override
            public String getName() {
                return "file-extensions-only";
            }
        };
        result = scoringEngine.evaluate(mr, List.of(configOnlyRule), NO_LLM);
    }

    @When("the scoring engine evaluates it with boost rules")
    public void scoringEngineEvaluatesWithBoostRules() {
        MergeRequest mr = buildMr();
        List<Rule> rules = new ArrayList<>();

        // Title keyword boost
        rules.add(createKeywordBoostRule());
        // Has tests boost
        rules.add(createHasTestsBoostRule());
        // Changed files range boost
        rules.add(createChangedFilesRangeBoostRule(3, 15));
        // Tech-debt label boost
        rules.add(createLabelBoostRule("tech-debt"));

        result = scoringEngine.evaluate(mr, rules, NO_LLM);
    }

    @When("the scoring engine evaluates it with penalty rules")
    public void scoringEngineEvaluatesWithPenaltyRules() {
        MergeRequest mr = buildMr();
        List<Rule> rules = new ArrayList<>();

        // Large diff penalty
        rules.add(createLargeDiffPenaltyRule(500));
        // No description penalty
        rules.add(createNoDescriptionPenaltyRule());
        // Config file penalty
        rules.add(createConfigFilePenaltyRule());

        result = scoringEngine.evaluate(mr, rules, NO_LLM);
    }

    @When("the scoring engine evaluates it with no matching rules")
    public void scoringEngineEvaluatesWithNoMatchingRules() {
        MergeRequest mr = buildMr();
        // Rules that won't match
        Rule nonMatchingRule = new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mergeRequest) {
                return new RuleResult("non-matching", false, 0.0, "did not match");
            }

            @Override
            public String getName() {
                return "non-matching";
            }
        };
        result = scoringEngine.evaluate(mr, List.of(nonMatchingRule), NO_LLM);
    }

    // --- Then steps ---

    @Then("the score should be {double}")
    public void scoreShouldBe(double expectedScore) {
        assertEquals(expectedScore, result.getScore(), 0.001);
    }

    @Then("the score should be greater than {double}")
    public void scoreShouldBeGreaterThan(double threshold) {
        assertTrue(result.getScore() > threshold,
                "Expected score > " + threshold + " but was " + result.getScore());
    }

    @Then("the score should be less than {double}")
    public void scoreShouldBeLessThan(double threshold) {
        assertTrue(result.getScore() < threshold,
                "Expected score < " + threshold + " but was " + result.getScore());
    }

    @Then("the verdict should be {word}")
    public void verdictShouldBe(String expectedVerdict) {
        assertEquals(Verdict.valueOf(expectedVerdict), result.getVerdict());
    }

    @Then("the verdict should be MAYBE or NOT_SUITABLE")
    public void verdictShouldBeMaybeOrNotSuitable() {
        assertTrue(result.getVerdict() == Verdict.MAYBE || result.getVerdict() == Verdict.NOT_SUITABLE,
                "Expected MAYBE or NOT_SUITABLE but was " + result.getVerdict());
    }

    @Then("the reasons should contain {string}")
    public void reasonsShouldContain(String expectedText) {
        assertTrue(result.getReasons().stream().anyMatch(r -> r.contains(expectedText)),
                "Expected reasons to contain '" + expectedText + "' but reasons were: " + result.getReasons());
    }

    // --- Helper methods to build MR and create test rules ---

    private MergeRequest buildMr() {
        if (changedFiles.isEmpty()) {
            changedFiles.add(new ChangedFile("src/Default.java", 10, 5, "modified"));
            mrBuilder.changedFiles(changedFiles);
            mrBuilder.diffStats(new DiffStats(10, 5, 1));
        }
        if (mrBuilder.build().getChangedFiles() == null) {
            mrBuilder.changedFiles(changedFiles);
        }
        return mrBuilder.build();
    }

    private List<Rule> createDefaultRules(MergeRequest mr) {
        List<Rule> rules = new ArrayList<>();

        // Label exclusion rule
        rules.add(createLabelExcludeRule(List.of("hotfix", "emergency", "do-not-automate")));
        // Keyword boost
        rules.add(createKeywordBoostRule());
        // Has tests boost
        rules.add(createHasTestsBoostRule());
        // Changed files range boost
        rules.add(createChangedFilesRangeBoostRule(3, 15));
        // Large diff penalty
        rules.add(createLargeDiffPenaltyRule(500));
        // No description penalty
        rules.add(createNoDescriptionPenaltyRule());

        return rules;
    }

    private Rule createLabelExcludeRule(List<String> excludedLabels) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                boolean matched = mr.getLabels() != null
                        && mr.getLabels().stream().anyMatch(excludedLabels::contains);
                String matchedLabel = matched
                        ? mr.getLabels().stream().filter(excludedLabels::contains).findFirst().orElse("")
                        : "";
                return new RuleResult("label-exclusion", matched,
                        matched ? ScoringEngine.EXCLUDE_WEIGHT : 0.0,
                        matched ? "excluded by label: " + matchedLabel : "no excluded labels");
            }

            @Override
            public String getName() {
                return "label-exclusion";
            }
        };
    }

    private Rule createMinFilesExcludeRule(int minFiles) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                int fileCount = mr.getChangedFiles() != null ? mr.getChangedFiles().size() : 0;
                boolean matched = fileCount < minFiles;
                return new RuleResult("min-changed-files", matched,
                        matched ? ScoringEngine.EXCLUDE_WEIGHT : 0.0,
                        matched ? "excluded: too few changed files (" + fileCount + " < " + minFiles + ")"
                                : "sufficient files changed");
            }

            @Override
            public String getName() {
                return "min-changed-files";
            }
        };
    }

    private Rule createKeywordBoostRule() {
        List<String> keywords = List.of("refactor", "cleanup", "simplify", "extract");
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                String titleLower = mr.getTitle() != null ? mr.getTitle().toLowerCase() : "";
                boolean matched = keywords.stream().anyMatch(titleLower::contains);
                return new RuleResult("title-keyword-boost", matched,
                        matched ? 0.1 : 0.0,
                        matched ? "title contains automation-friendly keyword" : "no keyword match");
            }

            @Override
            public String getName() {
                return "title-keyword-boost";
            }
        };
    }

    private Rule createHasTestsBoostRule() {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                boolean matched = mr.isHasTests();
                return new RuleResult("has-tests-boost", matched,
                        matched ? 0.1 : 0.0,
                        matched ? "merge request includes tests" : "no tests");
            }

            @Override
            public String getName() {
                return "has-tests-boost";
            }
        };
    }

    private Rule createChangedFilesRangeBoostRule(int min, int max) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                int count = mr.getChangedFiles() != null ? mr.getChangedFiles().size() : 0;
                boolean matched = count >= min && count <= max;
                return new RuleResult("changed-files-range-boost", matched,
                        matched ? 0.05 : 0.0,
                        matched ? "changed files count in ideal range" : "outside ideal range");
            }

            @Override
            public String getName() {
                return "changed-files-range-boost";
            }
        };
    }

    private Rule createLabelBoostRule(String label) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                boolean matched = mr.getLabels() != null && mr.getLabels().contains(label);
                return new RuleResult("label-boost-" + label, matched,
                        matched ? 0.05 : 0.0,
                        matched ? "has boost label: " + label : "no boost label");
            }

            @Override
            public String getName() {
                return "label-boost-" + label;
            }
        };
    }

    private Rule createLargeDiffPenaltyRule(int threshold) {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                int totalLines = mr.getDiffStats() != null
                        ? mr.getDiffStats().additions() + mr.getDiffStats().deletions() : 0;
                boolean matched = totalLines > threshold;
                return new RuleResult("large-diff-penalty", matched,
                        matched ? -0.15 : 0.0,
                        matched ? "diff too large (" + totalLines + " lines)" : "diff size acceptable");
            }

            @Override
            public String getName() {
                return "large-diff-penalty";
            }
        };
    }

    private Rule createNoDescriptionPenaltyRule() {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                boolean matched = mr.getDescription() == null || mr.getDescription().isBlank();
                return new RuleResult("no-description-penalty", matched,
                        matched ? -0.1 : 0.0,
                        matched ? "merge request has no description" : "has description");
            }

            @Override
            public String getName() {
                return "no-description-penalty";
            }
        };
    }

    private Rule createConfigFilePenaltyRule() {
        return new Rule() {
            @Override
            public RuleResult evaluate(MergeRequest mr) {
                List<String> configExtensions = List.of(".yml", ".yaml", ".toml", ".properties", ".xml");
                boolean matched = mr.getChangedFiles() != null
                        && mr.getChangedFiles().stream()
                        .anyMatch(f -> configExtensions.stream().anyMatch(ext -> f.path().endsWith(ext)));
                return new RuleResult("config-file-penalty", matched,
                        matched ? -0.05 : 0.0,
                        matched ? "touches config files" : "no config files changed");
            }

            @Override
            public String getName() {
                return "config-file-penalty";
            }
        };
    }
}
