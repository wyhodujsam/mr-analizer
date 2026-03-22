package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.AnalysisReportEntity;
import com.mranalizer.adapter.out.persistence.entity.AnalysisResultEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataAnalysisResultRepository extends JpaRepository<AnalysisReportEntity, Long> {

    @EntityGraph(attributePaths = {"results"})
    @Query("SELECT r FROM AnalysisReportEntity r WHERE r.id = :id")
    Optional<AnalysisReportEntity> findByIdWithResults(@Param("id") Long id);

    @Query("SELECT r FROM AnalysisResultEntity r WHERE r.report.id = :reportId AND r.id = :resultId")
    Optional<AnalysisResultEntity> findResultByReportIdAndId(@Param("reportId") Long reportId, @Param("resultId") Long resultId);

    List<AnalysisReportEntity> findByProjectSlug(String projectSlug);
}
