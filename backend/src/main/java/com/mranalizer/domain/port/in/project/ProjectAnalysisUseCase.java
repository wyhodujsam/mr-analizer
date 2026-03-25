package com.mranalizer.domain.port.in.project;

import com.mranalizer.domain.model.project.ProjectAnalysisResult;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface ProjectAnalysisUseCase {

    ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm);

    ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm,
                                          BiConsumer<Integer, Integer> progressCallback);

    List<ProjectAnalysisResult> getSavedAnalyses(String projectSlug);

    Optional<ProjectAnalysisResult> getSavedAnalysis(Long id);

    void deleteAnalysis(Long id);
}
