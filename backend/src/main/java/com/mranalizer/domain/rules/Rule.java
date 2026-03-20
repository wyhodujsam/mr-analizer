package com.mranalizer.domain.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.RuleResult;

/**
 * Domain interface for a single analysis rule.
 * Implementations evaluate a MergeRequest and return a RuleResult.
 * Pure domain — no framework dependencies.
 */
public interface Rule {
    RuleResult evaluate(MergeRequest mr);
    String getName();
}
