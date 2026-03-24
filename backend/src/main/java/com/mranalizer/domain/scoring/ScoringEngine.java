package com.mranalizer.domain.scoring;

import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;
import com.mranalizer.domain.model.Verdict;
import com.mranalizer.domain.rules.Rule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain service that scores a merge request against a set of rules and an LLM assessment.
 * No Spring or external framework dependencies.
 */
public class ScoringEngine {

    /** Sentinel weight value — any matched rule with weight <= this triggers immediate exclusion (hard). */
    public static final double EXCLUDE_WEIGHT = -1000.0;

    /** Weight for soft exclude rules (file-based) — heavy penalty but LLM can partially compensate. */
    public static final double SOFT_EXCLUDE_WEIGHT = -0.4;

    private final ScoringConfig config;

    public ScoringEngine(ScoringConfig config) {
        this.config = config;
    }

    public AnalysisResult evaluate(MergeRequest mr, List<Rule> rules, LlmAssessment llmAssessment) {
        List<RuleResult> ruleResults = rules.stream()
                .map(rule -> rule.evaluate(mr))
                .filter(RuleResult::matched)
                .toList();

        List<String> reasons = new ArrayList<>();
        List<String> matchedRuleNames = new ArrayList<>();

        boolean excluded = ruleResults.stream()
                .anyMatch(r -> r.weight() == EXCLUDE_WEIGHT);

        double score;
        Verdict verdict;

        if (excluded) {
            score = 0.0;
            verdict = Verdict.NOT_SUITABLE;
            ruleResults.stream()
                    .filter(r -> r.weight() == EXCLUDE_WEIGHT)
                    .forEach(r -> {
                        reasons.add(r.reason());
                        matchedRuleNames.add(r.ruleName());
                    });
        } else {
            double ruleAdjustment = ruleResults.stream()
                    .mapToDouble(RuleResult::weight)
                    .sum();

            score = Math.max(0.0, Math.min(1.0,
                    config.getBaseScore() + ruleAdjustment + llmAssessment.scoreAdjustment()));

            verdict = determineVerdict(score);

            for (RuleResult r : ruleResults) {
                String prefix = r.weight() > 0 ? "boost" : "penalize";
                reasons.add(prefix + ": " + r.reason() + " (" + (r.weight() > 0 ? "+" : "") + r.weight() + ")");
                matchedRuleNames.add(r.ruleName());
            }
        }

        String llmComment = llmAssessment.comment();
        if (llmAssessment.scoreAdjustment() != 0.0) {
            String commentText = llmComment != null ? llmComment : "no comment";
            reasons.add("llm: " + commentText + " ("
                    + (llmAssessment.scoreAdjustment() > 0 ? "+" : "")
                    + llmAssessment.scoreAdjustment() + ")");
        }

        return AnalysisResult.builder()
                .mergeRequest(mr)
                .score(score)
                .verdict(verdict)
                .reasons(reasons)
                .matchedRules(matchedRuleNames)
                .ruleResults(ruleResults)
                .llmComment(llmComment)
                .analyzedAt(LocalDateTime.now())
                .overallAutomatability(llmAssessment.overallAutomatability())
                .categories(llmAssessment.categories())
                .humanOversightRequired(llmAssessment.humanOversightRequired())
                .whyLlmFriendly(llmAssessment.whyLlmFriendly())
                .summaryTable(llmAssessment.summaryTable())
                .llmCost(llmAssessment.cost())
                .build();
    }

    private Verdict determineVerdict(double score) {
        if (score >= config.getAutomatableThreshold()) return Verdict.AUTOMATABLE;
        if (score >= config.getMaybeThreshold()) return Verdict.MAYBE;
        return Verdict.NOT_SUITABLE;
    }
}
