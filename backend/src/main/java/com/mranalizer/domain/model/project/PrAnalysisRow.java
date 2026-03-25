package com.mranalizer.domain.model.project;

import com.mranalizer.domain.model.RuleResult;
import com.mranalizer.domain.model.Verdict;

import java.time.LocalDateTime;
import java.util.List;

public record PrAnalysisRow(
        String prId,
        String title,
        String author,
        String state,
        String url,
        LocalDateTime createdAt,
        LocalDateTime mergedAt,
        int additions,
        int deletions,
        double aiScore,
        Verdict aiVerdict,
        List<RuleResult> ruleResults,
        String llmComment,
        boolean hasBdd,
        boolean hasSdd,
        List<String> bddFiles,
        List<String> sddFiles
) {}
