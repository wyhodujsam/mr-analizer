package com.mranalizer.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mranalizer.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaAnalysisResultRepositoryTest {

    @Autowired
    private SpringDataAnalysisResultRepository springRepo;

    private JpaAnalysisResultRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        repository = new JpaAnalysisResultRepository(springRepo, objectMapper);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MergeRequest buildMr() {
        return MergeRequest.builder()
                .externalId("123")
                .title("Refactor service layer")
                .author("jan.kowalski")
                .projectSlug("owner/repo")
                .provider("github")
                .url("https://github.com/owner/repo/pull/123")
                .description("Cleaned up the service layer for readability")
                .sourceBranch("feature/refactor")
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .mergedAt(LocalDateTime.of(2026, 1, 12, 14, 30))
                .labels(List.of("tech-debt", "refactoring"))
                .hasTests(true)
                .diffStats(new DiffStats(42, 18, 5))
                .build();
    }

    private AnalysisResult buildResult(Verdict verdict) {
        return AnalysisResult.builder()
                .mergeRequest(buildMr())
                .score(0.85)
                .verdict(verdict)
                .reasons(List.of("Small diff", "Has tests"))
                .matchedRules(List.of("boost:has-tests", "boost:description-keywords"))
                .llmComment("Good candidate for automation")
                .analyzedAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .overallAutomatability(85)
                .categories(List.of(
                        new AnalysisCategory("Code refactoring", 90, "Pure structural change"),
                        new AnalysisCategory("Test update", 80, "Straightforward test edits")
                ))
                .humanOversightRequired(List.of(
                        new HumanOversightItem("API contract", "Public interface changed")
                ))
                .whyLlmFriendly(List.of("Simple rename", "Mechanical change"))
                .summaryTable(List.of(
                        new SummaryAspect("Code execution", 95, "No logic change"),
                        new SummaryAspect("Risk", 10, "Low risk refactoring")
                ))
                .ruleResults(List.of(
                        new RuleResult("boost:has-tests", true, 0.15, "Tests present"),
                        new RuleResult("penalize:large-diff", false, 0.2, "Diff within limits")
                ))
                .build();
    }

    private AnalysisReport buildReport(String slug, Verdict verdict) {
        return AnalysisReport.of(
                null, slug, "github",
                LocalDateTime.of(2026, 3, 20, 10, 0),
                List.of(buildResult(verdict))
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void saveAndFindById_fullRoundTrip() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));

        assertThat(saved.getId()).isNotNull();
        Optional<AnalysisReport> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        AnalysisReport report = found.get();
        assertThat(report.getProjectSlug()).isEqualTo("owner/repo");
        assertThat(report.getProvider()).isEqualTo("github");
        assertThat(report.getTotalMrs()).isEqualTo(1);
        assertThat(report.getAutomatableCount()).isEqualTo(1);
        assertThat(report.getMaybeCount()).isEqualTo(0);
        assertThat(report.getNotSuitableCount()).isEqualTo(0);
        assertThat(report.getAnalyzedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 10, 0));

        assertThat(report.getResults()).hasSize(1);
        AnalysisResult result = report.getResults().get(0);
        assertThat(result.getScore()).isEqualTo(0.85);
        assertThat(result.getVerdict()).isEqualTo(Verdict.AUTOMATABLE);
        assertThat(result.getLlmComment()).isEqualTo("Good candidate for automation");
    }

    @Test
    void saveWithNullOptionalFields_noLlmData() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("456")
                .title("Quick fix")
                .author("dev")
                .projectSlug("owner/repo")
                .provider("github")
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .mergeRequest(mr)
                .score(0.3)
                .verdict(Verdict.NOT_SUITABLE)
                .reasons(List.of("Too large"))
                .matchedRules(List.of())
                .analyzedAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                .build();

        AnalysisReport report = AnalysisReport.of(
                null, "owner/repo", "github",
                LocalDateTime.of(2026, 3, 20, 11, 0),
                List.of(result)
        );

        AnalysisReport saved = repository.save(report);
        Optional<AnalysisReport> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        AnalysisResult loaded = found.get().getResults().get(0);
        assertThat(loaded.getOverallAutomatability()).isEqualTo(0);
        assertThat(loaded.getCategories()).isEmpty();
        assertThat(loaded.getHumanOversightRequired()).isEmpty();
        assertThat(loaded.getWhyLlmFriendly()).isEmpty();
        assertThat(loaded.getSummaryTable()).isEmpty();
        assertThat(loaded.getRuleResults()).isEmpty();
        assertThat(loaded.getLlmComment()).isNull();
    }

    @Test
    void findAll_returnsMultipleReports() {
        repository.save(buildReport("owner/repo-a", Verdict.AUTOMATABLE));
        repository.save(buildReport("owner/repo-b", Verdict.MAYBE));
        repository.save(buildReport("owner/repo-c", Verdict.NOT_SUITABLE));

        List<AnalysisReport> all = repository.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    void findByProjectSlug_filtersCorrectly() {
        repository.save(buildReport("owner/alpha", Verdict.AUTOMATABLE));
        repository.save(buildReport("owner/alpha", Verdict.MAYBE));
        repository.save(buildReport("owner/beta", Verdict.NOT_SUITABLE));

        List<AnalysisReport> alphaReports = repository.findByProjectSlug("owner/alpha");
        assertThat(alphaReports).hasSize(2);
        assertThat(alphaReports).allMatch(r -> r.getProjectSlug().equals("owner/alpha"));

        List<AnalysisReport> betaReports = repository.findByProjectSlug("owner/beta");
        assertThat(betaReports).hasSize(1);

        List<AnalysisReport> none = repository.findByProjectSlug("owner/nonexistent");
        assertThat(none).isEmpty();
    }

    @Test
    void findResult_returnsSingleResult() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));
        Long reportId = saved.getId();
        Long resultId = saved.getResults().get(0).getId();

        Optional<AnalysisResult> found = repository.findResult(reportId, resultId);
        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(0.85);
        assertThat(found.get().getVerdict()).isEqualTo(Verdict.AUTOMATABLE);
    }

    @Test
    void findResult_returnsEmptyForNonExistent() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));

        Optional<AnalysisResult> wrong = repository.findResult(saved.getId(), 99999L);
        assertThat(wrong).isEmpty();

        Optional<AnalysisResult> wrongReport = repository.findResult(99999L, 1L);
        assertThat(wrongReport).isEmpty();
    }

    @Test
    void deleteById_removesReport() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));
        assertThat(repository.findById(saved.getId())).isPresent();

        repository.deleteById(saved.getId());
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void save_preservesMrMetadata() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));
        AnalysisResult result = repository.findById(saved.getId()).orElseThrow().getResults().get(0);
        MergeRequest mr = result.getMergeRequest();

        assertThat(mr.getExternalId()).isEqualTo("123");
        assertThat(mr.getTitle()).isEqualTo("Refactor service layer");
        assertThat(mr.getAuthor()).isEqualTo("jan.kowalski");
        assertThat(mr.getProjectSlug()).isEqualTo("owner/repo");
        assertThat(mr.getProvider()).isEqualTo("github");
        assertThat(mr.getUrl()).isEqualTo("https://github.com/owner/repo/pull/123");
        assertThat(mr.getDescription()).isEqualTo("Cleaned up the service layer for readability");
        assertThat(mr.getSourceBranch()).isEqualTo("feature/refactor");
        assertThat(mr.getTargetBranch()).isEqualTo("main");
        assertThat(mr.getState()).isEqualTo("merged");
        assertThat(mr.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 10, 9, 0));
        assertThat(mr.getMergedAt()).isEqualTo(LocalDateTime.of(2026, 1, 12, 14, 30));
        assertThat(mr.getLabels()).containsExactly("tech-debt", "refactoring");
        assertThat(mr.hasTests()).isTrue();
        assertThat(mr.getDiffStats().additions()).isEqualTo(42);
        assertThat(mr.getDiffStats().deletions()).isEqualTo(18);
        assertThat(mr.getDiffStats().changedFilesCount()).isEqualTo(5);
    }

    @Test
    void save_preservesLlmDetailFields() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));
        AnalysisResult result = repository.findById(saved.getId()).orElseThrow().getResults().get(0);

        assertThat(result.getOverallAutomatability()).isEqualTo(85);

        assertThat(result.getCategories()).hasSize(2);
        assertThat(result.getCategories().get(0).name()).isEqualTo("Code refactoring");
        assertThat(result.getCategories().get(0).score()).isEqualTo(90);
        assertThat(result.getCategories().get(0).reasoning()).isEqualTo("Pure structural change");
        assertThat(result.getCategories().get(1).name()).isEqualTo("Test update");

        assertThat(result.getHumanOversightRequired()).hasSize(1);
        assertThat(result.getHumanOversightRequired().get(0).area()).isEqualTo("API contract");
        assertThat(result.getHumanOversightRequired().get(0).reasoning()).isEqualTo("Public interface changed");

        assertThat(result.getWhyLlmFriendly()).containsExactly("Simple rename", "Mechanical change");

        assertThat(result.getSummaryTable()).hasSize(2);
        assertThat(result.getSummaryTable().get(0).aspect()).isEqualTo("Code execution");
        assertThat(result.getSummaryTable().get(0).score()).isEqualTo(95);
        assertThat(result.getSummaryTable().get(0).note()).isEqualTo("No logic change");
    }

    @Test
    void save_preservesRuleResults() {
        AnalysisReport saved = repository.save(buildReport("owner/repo", Verdict.AUTOMATABLE));
        AnalysisResult result = repository.findById(saved.getId()).orElseThrow().getResults().get(0);

        assertThat(result.getRuleResults()).hasSize(2);
        RuleResult first = result.getRuleResults().get(0);
        assertThat(first.ruleName()).isEqualTo("boost:has-tests");
        assertThat(first.matched()).isTrue();
        assertThat(first.weight()).isEqualTo(0.15);
        assertThat(first.reason()).isEqualTo("Tests present");

        RuleResult second = result.getRuleResults().get(1);
        assertThat(second.ruleName()).isEqualTo("penalize:large-diff");
        assertThat(second.matched()).isFalse();
    }

    @Test
    void save_multipleResultsInOneReport() {
        AnalysisResult r1 = buildResult(Verdict.AUTOMATABLE);
        AnalysisResult r2 = AnalysisResult.builder()
                .mergeRequest(MergeRequest.builder()
                        .externalId("789")
                        .title("Add logging")
                        .author("dev2")
                        .projectSlug("owner/repo")
                        .provider("github")
                        .build())
                .score(0.45)
                .verdict(Verdict.MAYBE)
                .reasons(List.of("Medium complexity"))
                .matchedRules(List.of())
                .analyzedAt(LocalDateTime.of(2026, 3, 20, 10, 5))
                .build();

        AnalysisReport report = AnalysisReport.of(
                null, "owner/repo", "github",
                LocalDateTime.of(2026, 3, 20, 10, 0),
                List.of(r1, r2)
        );

        AnalysisReport saved = repository.save(report);
        AnalysisReport loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getResults()).hasSize(2);
        assertThat(loaded.getTotalMrs()).isEqualTo(2);
        assertThat(loaded.getAutomatableCount()).isEqualTo(1);
        assertThat(loaded.getMaybeCount()).isEqualTo(1);
    }

    @Test
    void findById_returnsEmptyForNonExistent() {
        assertThat(repository.findById(99999L)).isEmpty();
    }

    @Test
    void save_withNullMergeRequest() {
        AnalysisResult result = AnalysisResult.builder()
                .score(0.5)
                .verdict(Verdict.MAYBE)
                .reasons(List.of("Test"))
                .matchedRules(List.of())
                .analyzedAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                .build();

        AnalysisReport report = AnalysisReport.of(
                null, "owner/repo", "github",
                LocalDateTime.of(2026, 3, 20, 12, 0),
                List.of(result)
        );

        AnalysisReport saved = repository.save(report);
        AnalysisReport loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getResults()).hasSize(1);
        // MR fields should have defaults
        MergeRequest mr = loaded.getResults().get(0).getMergeRequest();
        assertThat(mr).isNotNull();
        assertThat(mr.getDiffStats()).isNotNull();
    }
}
