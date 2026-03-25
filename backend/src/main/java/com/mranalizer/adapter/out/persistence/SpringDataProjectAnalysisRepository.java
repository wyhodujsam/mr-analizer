package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.ProjectAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProjectAnalysisRepository extends JpaRepository<ProjectAnalysisEntity, Long> {

    List<ProjectAnalysisEntity> findByProjectSlugOrderByAnalyzedAtDesc(String projectSlug);

    List<ProjectAnalysisEntity> findAllByOrderByAnalyzedAtDesc();
}
