package com.mranalizer.adapter.out.persistence;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stub of {@link AnalysisResultRepository} used exclusively in tests.
 * Active only when the "test" Spring profile is enabled.
 * All other profiles use {@link JpaAnalysisResultRepository}.
 */
@Component
@Profile("test")
public class InMemoryAnalysisResultRepository implements AnalysisResultRepository {

    private final ConcurrentHashMap<Long, AnalysisReport> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public AnalysisReport save(AnalysisReport report) {
        Long id = (report.getId() != null) ? report.getId() : idSequence.getAndIncrement();

        // Re-build with assigned ID and assign IDs to nested results as well
        List<AnalysisResult> numberedResults = assignResultIds(report.getResults());

        AnalysisReport toStore = AnalysisReport.of(
                id,
                report.getProjectSlug(),
                report.getProvider(),
                report.getAnalyzedAt(),
                numberedResults
        );

        store.put(id, toStore);
        return toStore;
    }

    @Override
    public List<AnalysisReport> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<AnalysisReport> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public Optional<AnalysisResult> findResult(Long reportId, Long resultId) {
        return findById(reportId)
                .flatMap(report -> report.getResults().stream()
                        .filter(r -> resultId.equals(r.getId()))
                        .findFirst());
    }

    @Override
    public List<AnalysisReport> findByProjectSlug(String projectSlug) {
        return store.values().stream()
                .filter(r -> projectSlug.equals(r.getProjectSlug()))
                .toList();
    }

    /** Removes all stored reports. Used for test isolation between scenarios. */
    public void clear() {
        store.clear();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private final AtomicLong resultIdSequence = new AtomicLong(1);

    /**
     * Assigns sequential IDs to any result that does not yet have one.
     */
    private List<AnalysisResult> assignResultIds(List<AnalysisResult> results) {
        if (results == null) {
            return List.of();
        }
        List<AnalysisResult> numbered = new ArrayList<>(results.size());
        for (AnalysisResult r : results) {
            if (r.getId() != null) {
                numbered.add(r);
            } else {
                numbered.add(AnalysisResult.builder()
                        .id(resultIdSequence.getAndIncrement())
                        .mergeRequest(r.getMergeRequest())
                        .score(r.getScore())
                        .verdict(r.getVerdict())
                        .reasons(r.getReasons())
                        .matchedRules(r.getMatchedRules())
                        .llmComment(r.getLlmComment())
                        .analyzedAt(r.getAnalyzedAt())
                        .overallAutomatability(r.getOverallAutomatability())
                        .categories(r.getCategories())
                        .humanOversightRequired(r.getHumanOversightRequired())
                        .whyLlmFriendly(r.getWhyLlmFriendly())
                        .summaryTable(r.getSummaryTable())
                        .build());
            }
        }
        return numbered;
    }
}
