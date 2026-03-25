package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.ProjectAnalysisEntity;
import com.mranalizer.domain.model.project.*;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaProjectAnalysisRepository implements ProjectAnalysisRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaProjectAnalysisRepository.class);

    private final SpringDataProjectAnalysisRepository springRepo;
    private final ObjectMapper objectMapper;

    public JpaProjectAnalysisRepository(SpringDataProjectAnalysisRepository springRepo) {
        this.springRepo = springRepo;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public ProjectAnalysisResult save(ProjectAnalysisResult result) {
        ProjectAnalysisEntity entity = toEntity(result);
        ProjectAnalysisEntity saved = springRepo.save(entity);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public List<ProjectAnalysisResult> findAll() {
        return springRepo.findAllByOrderByAnalyzedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProjectAnalysisResult> findByProjectSlug(String projectSlug) {
        return springRepo.findByProjectSlugOrderByAnalyzedAtDesc(projectSlug).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProjectAnalysisResult> findById(Long id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        springRepo.deleteById(id);
    }

    private ProjectAnalysisEntity toEntity(ProjectAnalysisResult result) {
        ProjectAnalysisEntity e = new ProjectAnalysisEntity();
        e.setProjectSlug(result.getProjectSlug());
        e.setAnalyzedAt(result.getAnalyzedAt());

        ProjectSummary s = result.getSummary();
        e.setTotalPrs(s.totalPrs());
        e.setAutomatableCount(s.automatableCount());
        e.setMaybeCount(s.maybeCount());
        e.setNotSuitableCount(s.notSuitableCount());
        e.setAvgScore(s.avgScore());
        e.setBddCount(s.bddCount());
        e.setBddPercent(s.bddPercent());
        e.setSddCount(s.sddCount());
        e.setSddPercent(s.sddPercent());

        try {
            e.setSummaryJson(objectMapper.writeValueAsString(s));
            e.setRowsJson(objectMapper.writeValueAsString(result.getRows()));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize project analysis: " + ex.getMessage(), ex);
        }

        return e;
    }

    private ProjectAnalysisResult toDomain(ProjectAnalysisEntity e) {
        try {
            ProjectSummary summary = objectMapper.readValue(e.getSummaryJson(), ProjectSummary.class);
            List<PrAnalysisRow> rows = objectMapper.readValue(e.getRowsJson(),
                    new TypeReference<List<PrAnalysisRow>>() {});
            return new ProjectAnalysisResult(e.getId(), e.getProjectSlug(), e.getAnalyzedAt(), rows, summary);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize project analysis {}: {}", e.getId(), ex.getMessage());
            ProjectSummary empty = new ProjectSummary(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), 0, 0, 0, 0);
            return new ProjectAnalysisResult(e.getId(), e.getProjectSlug(), e.getAnalyzedAt(), List.of(), empty);
        }
    }
}
