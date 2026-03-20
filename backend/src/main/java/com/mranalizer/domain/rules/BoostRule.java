package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;

import java.util.List;
import java.util.function.Function;

/**
 * Rule that boosts the score of a merge request when matched.
 * Weight should be positive.
 * Pure domain — no framework dependencies.
 */
public class BoostRule implements Rule {

    private final String name;
    private final Function<MergeRequest, RuleResult> evaluator;

    private BoostRule(String name, Function<MergeRequest, RuleResult> evaluator) {
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

    public static BoostRule byTitleKeywords(List<String> keywords, double weight) {
        String ruleName = "boost-by-title-keywords";
        return new BoostRule(ruleName, mr -> {
            String title = mr.getTitle() != null ? mr.getTitle().toLowerCase() : "";
            String description = mr.getDescription() != null ? mr.getDescription().toLowerCase() : "";
            for (String keyword : keywords) {
                String kw = keyword.toLowerCase();
                if (title.contains(kw) || description.contains(kw)) {
                    return matched(ruleName, weight, "title/description contains keyword: " + keyword);
                }
            }
            return notMatched(ruleName);
        });
    }

    public static BoostRule byDescriptionKeywords(List<String> keywords, double weight) {
        return byTitleKeywords(keywords, weight);
    }

    public static BoostRule byHasTests(double weight) {
        String ruleName = "boost-by-has-tests";
        return new BoostRule(ruleName, mr -> {
            if (mr.isHasTests()) {
                return matched(ruleName, weight, "PR includes test files");
            }
            return notMatched(ruleName);
        });
    }

    public static BoostRule byChangedFilesRange(int min, int max, double weight) {
        String ruleName = "boost-by-changed-files-range";
        return new BoostRule(ruleName, mr -> {
            int count = mr.getDiffStats().changedFilesCount();
            if (count >= min && count <= max) {
                return matched(ruleName, weight, "changed files count " + count + " in range [" + min + ", " + max + "]");
            }
            return notMatched(ruleName);
        });
    }

    public static BoostRule byLabels(List<String> labels, double weight) {
        String ruleName = "boost-by-labels";
        return new BoostRule(ruleName, mr -> {
            for (String label : mr.getLabels()) {
                if (labels.contains(label)) {
                    return matched(ruleName, weight, "PR has boosting label: " + label);
                }
            }
            return notMatched(ruleName);
        });
    }
}
