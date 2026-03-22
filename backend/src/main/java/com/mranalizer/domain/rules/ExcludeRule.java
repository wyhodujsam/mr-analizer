package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;
import com.mranalizer.domain.scoring.ScoringEngine;

import java.util.List;
import java.util.function.Function;

/**
 * Rule that excludes a merge request from scoring entirely.
 * When matched, returns a RuleResult with EXCLUDE_WEIGHT sentinel value.
 * Pure domain — no framework dependencies.
 */
public class ExcludeRule implements Rule {

    private final String name;
    private final Function<MergeRequest, RuleResult> evaluator;

    private ExcludeRule(String name, Function<MergeRequest, RuleResult> evaluator) {
        this.name = name;
        this.evaluator = evaluator;
    }

    @Override
    public RuleResult evaluate(MergeRequest mr) {
        return evaluator.apply(mr);
    }

    @Override
    public String getName() {
        return name;
    }

    private static RuleResult hardExcluded(String ruleName, String reason) {
        return new RuleResult(ruleName, true, ScoringEngine.EXCLUDE_WEIGHT, reason);
    }

    private static RuleResult softExcluded(String ruleName, String reason) {
        return new RuleResult(ruleName, true, ScoringEngine.SOFT_EXCLUDE_WEIGHT, reason);
    }

    private static RuleResult notMatched(String ruleName) {
        return new RuleResult(ruleName, false, 0.0, "");
    }

    public static ExcludeRule byLabels(List<String> excludedLabels) {
        String ruleName = "exclude-by-labels";
        return new ExcludeRule(ruleName, mr -> {
            for (String label : mr.getLabels()) {
                if (excludedLabels.contains(label)) {
                    return hardExcluded(ruleName, "excluded by label: " + label);
                }
            }
            return notMatched(ruleName);
        });
    }

    public static ExcludeRule byMinChangedFiles(int min) {
        String ruleName = "soft-exclude-by-min-changed-files";
        return new ExcludeRule(ruleName, mr -> {
            int count = mr.getDiffStats().changedFilesCount();
            if (count < min) {
                return softExcluded(ruleName, "soft-exclude: only " + count + " changed files (min: " + min + ")");
            }
            return notMatched(ruleName);
        });
    }

    public static ExcludeRule byMaxChangedFiles(int max) {
        String ruleName = "soft-exclude-by-max-changed-files";
        return new ExcludeRule(ruleName, mr -> {
            int count = mr.getDiffStats().changedFilesCount();
            if (count > max) {
                return softExcluded(ruleName, "soft-exclude: " + count + " changed files exceeds max " + max);
            }
            return notMatched(ruleName);
        });
    }

    public static ExcludeRule byFileExtensionsOnly(List<String> extensions) {
        String ruleName = "soft-exclude-by-file-extensions-only";
        return new ExcludeRule(ruleName, mr -> {
            var files = mr.getChangedFiles();
            if (files.isEmpty()) {
                return notMatched(ruleName);
            }
            boolean allMatch = files.stream().allMatch(f -> {
                String path = f.path();
                return extensions.stream().anyMatch(path::endsWith);
            });
            if (allMatch) {
                return softExcluded(ruleName, "soft-exclude: all changed files have excluded extensions");
            }
            return notMatched(ruleName);
        });
    }
}
