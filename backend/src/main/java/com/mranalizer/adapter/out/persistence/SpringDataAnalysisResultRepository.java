package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.AnalysisReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAnalysisResultRepository extends JpaRepository<AnalysisReportEntity, Long> {
}
