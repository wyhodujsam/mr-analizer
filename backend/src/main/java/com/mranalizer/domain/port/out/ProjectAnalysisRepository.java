package com.mranalizer.domain.port.out;

import com.mranalizer.domain.model.project.ProjectAnalysisResult;

import java.util.List;
import java.util.Optional;

public interface ProjectAnalysisRepository {

    ProjectAnalysisResult save(ProjectAnalysisResult result);

    List<ProjectAnalysisResult> findAll();

    List<ProjectAnalysisResult> findByProjectSlug(String projectSlug);

    Optional<ProjectAnalysisResult> findById(Long id);

    void deleteById(Long id);
}
