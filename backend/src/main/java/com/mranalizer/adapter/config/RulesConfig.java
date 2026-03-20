package com.mranalizer.adapter.config;

import com.mranalizer.domain.rules.BoostRule;
import com.mranalizer.domain.rules.ExcludeRule;
import com.mranalizer.domain.rules.PenalizeRule;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring configuration that reads rule settings from application.yml
 * and creates Rule beans and ScoringEngine bean.
 */
@Configuration
public class RulesConfig {

    // Scoring
    @Value("${mr-analizer.scoring.base-score:0.5}")
    private double baseScore;

    @Value("${mr-analizer.scoring.automatable-threshold:0.7}")
    private double automatableThreshold;

    @Value("${mr-analizer.scoring.maybe-threshold:0.4}")
    private double maybeThreshold;

    // Exclude rules
    @Value("${mr-analizer.rules.exclude.min-changed-files:2}")
    private int excludeMinChangedFiles;

    @Value("${mr-analizer.rules.exclude.max-changed-files:50}")
    private int excludeMaxChangedFiles;

    @Value("${mr-analizer.rules.exclude.file-extensions-only:}")
    private List<String> excludeFileExtensionsOnly;

    @Value("${mr-analizer.rules.exclude.labels:}")
    private List<String> excludeLabels;

    // Boost rules
    @Value("${mr-analizer.rules.boost.description-keywords.words:}")
    private List<String> boostDescriptionKeywords;

    @Value("${mr-analizer.rules.boost.description-keywords.weight:0.2}")
    private double boostDescriptionKeywordsWeight;

    @Value("${mr-analizer.rules.boost.has-tests.weight:0.15}")
    private double boostHasTestsWeight;

    @Value("${mr-analizer.rules.boost.changed-files-range.min:3}")
    private int boostChangedFilesMin;

    @Value("${mr-analizer.rules.boost.changed-files-range.max:15}")
    private int boostChangedFilesMax;

    @Value("${mr-analizer.rules.boost.changed-files-range.weight:0.1}")
    private double boostChangedFilesWeight;

    @Value("${mr-analizer.rules.boost.labels.values:}")
    private List<String> boostLabels;

    @Value("${mr-analizer.rules.boost.labels.weight:0.15}")
    private double boostLabelsWeight;

    // Penalize rules
    @Value("${mr-analizer.rules.penalize.large-diff.threshold:500}")
    private int penalizeLargeDiffThreshold;

    @Value("${mr-analizer.rules.penalize.large-diff.weight:0.2}")
    private double penalizeLargeDiffWeight;

    @Value("${mr-analizer.rules.penalize.no-description.weight:0.3}")
    private double penalizeNoDescriptionWeight;

    @Value("${mr-analizer.rules.penalize.touches-config.weight:0.1}")
    private double penalizeTouchesConfigWeight;

    @Bean
    public ScoringConfig scoringConfig() {
        return new ScoringConfig(baseScore, automatableThreshold, maybeThreshold);
    }

    @Bean
    public ScoringEngine scoringEngine(ScoringConfig scoringConfig) {
        return new ScoringEngine(scoringConfig);
    }

    @Bean
    public List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // Exclude rules
        if (excludeLabels != null && !excludeLabels.isEmpty()) {
            rules.add(ExcludeRule.byLabels(excludeLabels));
        }
        rules.add(ExcludeRule.byMinChangedFiles(excludeMinChangedFiles));
        rules.add(ExcludeRule.byMaxChangedFiles(excludeMaxChangedFiles));
        if (excludeFileExtensionsOnly != null && !excludeFileExtensionsOnly.isEmpty()) {
            rules.add(ExcludeRule.byFileExtensionsOnly(excludeFileExtensionsOnly));
        }

        // Boost rules
        if (boostDescriptionKeywords != null && !boostDescriptionKeywords.isEmpty()) {
            rules.add(BoostRule.byTitleKeywords(boostDescriptionKeywords, boostDescriptionKeywordsWeight));
        }
        rules.add(BoostRule.byHasTests(boostHasTestsWeight));
        rules.add(BoostRule.byChangedFilesRange(boostChangedFilesMin, boostChangedFilesMax, boostChangedFilesWeight));
        if (boostLabels != null && !boostLabels.isEmpty()) {
            rules.add(BoostRule.byLabels(boostLabels, boostLabelsWeight));
        }

        // Penalize rules
        rules.add(PenalizeRule.byLargeDiff(penalizeLargeDiffThreshold, -penalizeLargeDiffWeight));
        rules.add(PenalizeRule.byNoDescription(-penalizeNoDescriptionWeight));
        rules.add(PenalizeRule.byTouchesConfig(-penalizeTouchesConfigWeight));

        return rules;
    }
}
