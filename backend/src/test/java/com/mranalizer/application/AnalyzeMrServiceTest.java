package com.mranalizer.application;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.rules.BoostRule;
import com.mranalizer.domain.rules.ExcludeRule;
import com.mranalizer.domain.rules.PenalizeRule;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AnalyzeMrServiceTest {

    @Mock
    private MergeRequestProvider provider;

    @Mock
    private LlmAnalyzer llmAnalyzer;

    @Mock
    private AnalysisResultRepository repository;

    @Mock
    private ManageReposUseCase manageReposUseCase;

    private ScoringEngine scoringEngine;
    private List<Rule> rules;
    private AnalyzeMrService service;

    @BeforeEach
    void setUp() {
        ScoringConfig config = new ScoringConfig(0.5, 0.7, 0.4);
        scoringEngine = new ScoringEngine(config);

        rules = List.of(
                ExcludeRule.byLabels(List.of("hotfix", "security")),
                BoostRule.byDescriptionKeywords(List.of("refactor", "cleanup"), 0.2),
                BoostRule.byHasTests(0.15),
                PenalizeRule.byNoDescription(-0.3)
        );

        // Default: no cached analysis (cache miss)
        lenient().when(repository.findByProjectSlug(any())).thenReturn(Optional.empty());

        service = new AnalyzeMrService(provider, llmAnalyzer, repository, scoringEngine, rules, manageReposUseCase);
    }

    @Test
    void analyze_callsProviderWithCriteria() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(50)
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of());
        when(provider.getProviderName()).thenReturn("github");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(criteria, false);

        ArgumentCaptor<FetchCriteria> captor = ArgumentCaptor.forClass(FetchCriteria.class);
        verify(provider).fetchMergeRequests(captor.capture());

        FetchCriteria captured = captor.getValue();
        assertThat(captured.getProjectSlug()).isEqualTo("owner/repo");
        assertThat(captured.getState()).isEqualTo("merged");
        assertThat(captured.getLimit()).isEqualTo(50);
    }

    @Test
    void analyze_scoresAllMrs() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .build();

        List<MergeRequest> mrs = List.of(
                buildMr(1L, "MR 1", "refactor code", 5),
                buildMr(2L, "MR 2", "add feature", 8),
                buildMr(3L, "MR 3", "cleanup tests", 3)
        );

        when(provider.fetchMergeRequests(any())).thenReturn(mrs);
        when(provider.getProviderName()).thenReturn("github");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisReport report = service.analyze(criteria, false);

        assertThat(report.getTotalMrs()).isEqualTo(3);
        assertThat(report.getResults()).hasSize(3);
    }

    @Test
    void analyze_savesReport() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of(buildMr(1L, "MR 1", "desc", 5)));
        when(provider.getProviderName()).thenReturn("github");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(criteria, false);

        ArgumentCaptor<AnalysisReport> captor = ArgumentCaptor.forClass(AnalysisReport.class);
        verify(repository).save(captor.capture());

        AnalysisReport saved = captor.getValue();
        assertThat(saved.getProjectSlug()).isEqualTo("owner/repo");
        assertThat(saved.getProvider()).isEqualTo("github");
        assertThat(saved.getResults()).hasSize(1);
    }

    @Test
    void analyze_withLlm_callsLlmAnalyzer() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .build();

        List<MergeRequest> mrs = List.of(
                buildMr(1L, "MR 1", "desc1", 5),
                buildMr(2L, "MR 2", "desc2", 8)
        );

        when(provider.fetchMergeRequests(any())).thenReturn(mrs);
        when(provider.getProviderName()).thenReturn("github");
        when(llmAnalyzer.analyze(any())).thenReturn(new LlmAssessment(0.1, "looks good", "test-llm"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(criteria, true);

        verify(llmAnalyzer, times(2)).analyze(any(MergeRequest.class));
    }

    @Test
    void analyze_withoutLlm_skipsLlmAnalyzer() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of(buildMr(1L, "MR 1", "desc", 5)));
        when(provider.getProviderName()).thenReturn("github");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(criteria, false);

        verify(llmAnalyzer, never()).analyze(any(MergeRequest.class));
    }

    @Test
    void analyze_llmError_continuesWithNeutralAssessment() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of(buildMr(1L, "MR 1", "desc", 5)));
        when(provider.getProviderName()).thenReturn("github");
        when(llmAnalyzer.analyze(any())).thenThrow(new RuntimeException("LLM service down"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisReport report = service.analyze(criteria, true);

        assertThat(report.getResults()).hasSize(1);
        AnalysisResult result = report.getResults().get(0);
        assertThat(result.getLlmComment()).contains("LLM error");
    }

    @Test
    void getReport_returnsFromRepository() {
        AnalysisReport expected = AnalysisReport.of(1L, "owner/repo", "github", LocalDateTime.now(), List.of());
        when(repository.findById(1L)).thenReturn(Optional.of(expected));

        Optional<AnalysisReport> result = service.getReport(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(repository).findById(1L);
    }

    @Test
    void getResult_findsInReport() {
        AnalysisResult ar = AnalysisResult.builder()
                .id(10L)
                .mergeRequest(buildMr(1L, "MR 1", "desc", 5))
                .score(0.8)
                .verdict(Verdict.AUTOMATABLE)
                .reasons(List.of("test"))
                .matchedRules(List.of("rule1"))
                .analyzedAt(LocalDateTime.now())
                .build();

        AnalysisReport report = AnalysisReport.of(1L, "owner/repo", "github", LocalDateTime.now(), List.of(ar));
        when(repository.findById(1L)).thenReturn(Optional.of(report));

        Optional<AnalysisResult> result = service.getResult(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getScore()).isEqualTo(0.8);
    }

    // -------------------------------------------------------------------------
    // Cache & delete tests (feature 002-mr-browse-analyze)
    // -------------------------------------------------------------------------

    @Test
    void analyze_returnsCachedWhenExists() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .build();

        AnalysisReport cached = AnalysisReport.of(
                99L, "owner/repo", "github", LocalDateTime.now(), List.of());
        when(repository.findByProjectSlug("owner/repo")).thenReturn(Optional.of(cached));

        AnalysisReport result = service.analyze(criteria, false);

        assertThat(result.getId()).isEqualTo(99L);
        verify(provider, never()).fetchMergeRequests(any());
    }

    @Test
    void analyze_calculatesWhenNoCache() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .build();

        when(repository.findByProjectSlug("owner/repo")).thenReturn(Optional.empty());
        when(provider.fetchMergeRequests(any())).thenReturn(List.of(buildMr(1L, "MR 1", "desc", 5)));
        when(provider.getProviderName()).thenReturn("github");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalysisReport result = service.analyze(criteria, false);

        assertThat(result.getResults()).hasSize(1);
        verify(provider).fetchMergeRequests(any());
    }

    @Test
    void deleteAnalysis_delegatesToRepository() {
        service.deleteAnalysis(42L);

        verify(repository).deleteById(42L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MergeRequest buildMr(Long id, String title, String description, int changedFiles) {
        return MergeRequest.builder()
                .id(id)
                .externalId(String.valueOf(id))
                .title(title)
                .description(description)
                .author("testauthor")
                .sourceBranch("feature/test")
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(1))
                .mergedAt(LocalDateTime.now())
                .labels(List.of())
                .changedFiles(buildChangedFiles(changedFiles))
                .diffStats(new DiffStats(changedFiles * 10, changedFiles * 3, changedFiles))
                .hasTests(true)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(2)
                .provider("github")
                .url("https://github.com/owner/repo/pull/" + id)
                .projectSlug("owner/repo")
                .build();
    }

    private List<ChangedFile> buildChangedFiles(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new ChangedFile("src/main/File" + i + ".java", 10, 3, "modified"))
                .toList();
    }
}
