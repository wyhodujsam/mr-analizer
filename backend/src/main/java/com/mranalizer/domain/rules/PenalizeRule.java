package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;

import java.util.List;
import java.util.function.Function;

/**
 * Rule that penalizes (lowers) the score of a merge request when matched.
 * Weight should be negative.
 * Pure domain — no framework dependencies.
 */
public class PenalizeRule implements Rule {

    private final String name;
    private final Function<MergeRequest, RuleResult> evaluator;

    private PenalizeRule(String name, Function<MergeRequest, RuleResult> evaluator) {
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

    private static RuleResult matched(String ruleName, double weight, String reason) {
        return new RuleResult(ruleName, true, weight, reason);
    }

    private static RuleResult notMatched(String ruleName) {
        return new RuleResult(ruleName, false, 0.0, "");
    }

    public static PenalizeRule byLargeDiff(int threshold, double weight) {
        String ruleName = "penalize-by-large-diff";
        return new PenalizeRule(ruleName, mr -> {
            int totalLines = mr.getDiffStats().additions() + mr.getDiffStats().deletions();
            if (totalLines > threshold) {
                return matched(ruleName, weight, "diff size " + totalLines + " exceeds threshold " + threshold);
            }
            return notMatched(ruleName);
        });
    }

    public static PenalizeRule byNoDescription(double weight) {
        String ruleName = "penalize-by-no-description";
        return new PenalizeRule(ruleName, mr -> {
            String desc = mr.getDescription();
            if (desc == null || desc.isBlank()) {
                return matched(ruleName, weight, "PR has no description");
            }
            return notMatched(ruleName);
        });
    }

    public static PenalizeRule byTouchesConfig(List<String> extensions, double weight) {
        String ruleName = "penalize-by-touches-config";
        return new PenalizeRule(ruleName, mr -> {
            boolean touchesConfig = mr.getChangedFiles().stream()
                    .anyMatch(f -> extensions.stream().anyMatch(ext -> f.path().endsWith(ext)));
            if (touchesConfig) {
                return matched(ruleName, weight, "PR touches config files");
            }
            return notMatched(ruleName);
        });
    }

    public static PenalizeRule byTouchesConfig(double weight) {
        return byTouchesConfig(
                List.of(".yml", ".yaml", ".toml", ".env", ".properties", ".xml", ".json"),
                weight);
    }
}
