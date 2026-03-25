package com.mranalizer.adapter.config;

import com.mranalizer.domain.model.project.DetectionPatterns;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringEngine;
import com.mranalizer.domain.service.activity.ActivityAnalysisService;
import com.mranalizer.domain.service.project.ArtifactDetector;
import com.mranalizer.domain.service.project.ProjectAnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ProjectConfig {

    @Value("${mr-analizer.detection.bdd-patterns:*.feature,*Steps.java,*_steps.py,*_steps.rb,*.steps.ts,*Steps.kt}")
    private List<String> bddPatterns;

    @Value("${mr-analizer.detection.sdd-patterns:spec.md,plan.md,tasks.md,research.md,quickstart.md,checklist.md}")
    private List<String> sddPatterns;

    @Bean
    public DetectionPatterns detectionPatterns() {
        return new DetectionPatterns(bddPatterns, sddPatterns);
    }

    @Bean
    public ArtifactDetector artifactDetector(DetectionPatterns detectionPatterns) {
        return new ArtifactDetector(detectionPatterns);
    }

    @Bean(destroyMethod = "close")
    public ProjectAnalysisService projectAnalysisService(
            ActivityAnalysisService activityService,
            MergeRequestProvider mergeRequestProvider,
            ScoringEngine scoringEngine,
            List<Rule> rules,
            ArtifactDetector artifactDetector,
            LlmAnalyzer llmAnalyzer,
            ProjectAnalysisRepository repository) {
        return new ProjectAnalysisService(
                activityService, mergeRequestProvider, scoringEngine, rules,
                artifactDetector, llmAnalyzer, repository);
    }
}
