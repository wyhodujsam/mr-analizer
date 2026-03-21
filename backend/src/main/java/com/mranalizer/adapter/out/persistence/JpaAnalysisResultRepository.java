package com.mranalizer.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.adapter.out.persistence.entity.AnalysisReportEntity;
import com.mranalizer.adapter.out.persistence.entity.AnalysisResultEntity;
import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.Verdict;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link AnalysisResultRepository}.
 * Active on all profiles except "test" — the default production implementation.
 * In the "test" profile, {@link InMemoryAnalysisResultRepository} is used instead.
 */
@Component
@Profile("!test")
public class JpaAnalysisResultRepository implements AnalysisResultRepository {

    private final SpringDataAnalysisResultRepository springRepo;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    public JpaAnalysisResultRepository(SpringDataAnalysisResultRepository springRepo,
                                       ObjectMapper objectMapper) {
        this.springRepo = springRepo;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Port implementation
    // -------------------------------------------------------------------------

    @Override
    public AnalysisReport save(AnalysisReport report) {
        AnalysisReportEntity entity = toEntity(report);
        AnalysisReportEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AnalysisReport> findAll() {
        return springRepo.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AnalysisReport> findById(Long id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        springRepo.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Mapping: domain -> entity
    // -------------------------------------------------------------------------

    private AnalysisReportEntity toEntity(AnalysisReport report) {
        AnalysisReportEntity entity = new AnalysisReportEntity();
        entity.setId(report.getId());
        entity.setProjectSlug(report.getProjectSlug());
        entity.setProvider(report.getProvider());
        entity.setAnalyzedAt(report.getAnalyzedAt());
        entity.setTotalMrs(report.getTotalMrs());
        entity.setAutomatableCount(report.getAutomatableCount());
        entity.setMaybeCount(report.getMaybeCount());
        entity.setNotSuitableCount(report.getNotSuitableCount());

        List<AnalysisResult> results = report.getResults();
        if (results != null) {
            List<AnalysisResultEntity> resultEntities = new ArrayList<>();
            for (AnalysisResult r : results) {
                AnalysisResultEntity re = toResultEntity(r, entity);
                resultEntities.add(re);
            }
            entity.setResults(resultEntities);
        }

        return entity;
    }

    private AnalysisResultEntity toResultEntity(AnalysisResult result, AnalysisReportEntity reportEntity) {
        AnalysisResultEntity entity = new AnalysisResultEntity();
        entity.setId(result.getId());
        entity.setScore(result.getScore());
        entity.setVerdict(result.getVerdict() != null ? result.getVerdict().name() : null);
        entity.setReasons(toJson(result.getReasons()));
        entity.setMatchedRules(toJson(result.getMatchedRules()));
        entity.setLlmComment(result.getLlmComment());
        entity.setAnalyzedAt(result.getAnalyzedAt());
        entity.setReport(reportEntity);

        MergeRequest mr = result.getMergeRequest();
        if (mr != null) {
            entity.setExternalMrId(mr.getExternalId());
            entity.setMrTitle(mr.getTitle());
            entity.setMrAuthor(mr.getAuthor());
            entity.setProjectSlug(mr.getProjectSlug());
            entity.setProvider(mr.getProvider());
            entity.setMrUrl(mr.getUrl());
        }

        return entity;
    }

    // -------------------------------------------------------------------------
    // Mapping: entity -> domain
    // -------------------------------------------------------------------------

    private AnalysisReport toDomain(AnalysisReportEntity entity) {
        List<AnalysisResult> results = new ArrayList<>();
        if (entity.getResults() != null) {
            for (AnalysisResultEntity re : entity.getResults()) {
                results.add(toResultDomain(re));
            }
        }
        return AnalysisReport.of(
                entity.getId(),
                entity.getProjectSlug(),
                entity.getProvider(),
                entity.getAnalyzedAt(),
                results
        );
    }

    private AnalysisResult toResultDomain(AnalysisResultEntity entity) {
        MergeRequest mr = MergeRequest.builder()
                .externalId(entity.getExternalMrId())
                .title(entity.getMrTitle())
                .author(entity.getMrAuthor())
                .projectSlug(entity.getProjectSlug())
                .provider(entity.getProvider())
                .url(entity.getMrUrl())
                .build();

        Verdict verdict = null;
        if (entity.getVerdict() != null) {
            verdict = Verdict.valueOf(entity.getVerdict());
        }

        return AnalysisResult.builder()
                .id(entity.getId())
                .mergeRequest(mr)
                .score(entity.getScore())
                .verdict(verdict)
                .reasons(fromJson(entity.getReasons()))
                .matchedRules(fromJson(entity.getMatchedRules()))
                .llmComment(entity.getLlmComment())
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------

    private String toJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize list to JSON", e);
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, LIST_OF_STRING);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize JSON to list", e);
        }
    }
}
