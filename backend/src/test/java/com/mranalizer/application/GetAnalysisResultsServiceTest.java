package com.mranalizer.application;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAnalysisResultsServiceTest {

    @Mock
    private AnalysisResultRepository repository;

    private GetAnalysisResultsService service;

    @BeforeEach
    void setUp() {
        service = new GetAnalysisResultsService(repository);
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
    void getReport_returnsEmptyWhenNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<AnalysisReport> result = service.getReport(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getResult_findsInReport() {
        MergeRequest mr = MergeRequest.builder()
                .id(1L).externalId("1").title("MR 1").description("desc")
                .author("author").sourceBranch("feature").targetBranch("main")
                .state("merged").createdAt(LocalDateTime.now()).labels(List.of())
                .changedFiles(List.of()).diffStats(new DiffStats(10, 3, 1))
                .hasTests(true).provider("github").projectSlug("owner/repo")
                .build();

        AnalysisResult ar = AnalysisResult.builder()
                .id(10L).mergeRequest(mr).score(0.8).verdict(Verdict.AUTOMATABLE)
                .reasons(List.of("test")).matchedRules(List.of("rule1"))
                .analyzedAt(LocalDateTime.now()).build();

        when(repository.findResult(1L, 10L)).thenReturn(Optional.of(ar));

        Optional<AnalysisResult> result = service.getResult(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getScore()).isEqualTo(0.8);
        verify(repository).findResult(1L, 10L);
    }

    @Test
    void getResult_returnsEmptyWhenResultNotInReport() {
        when(repository.findResult(1L, 999L)).thenReturn(Optional.empty());

        Optional<AnalysisResult> result = service.getResult(1L, 999L);

        assertThat(result).isEmpty();
        verify(repository).findResult(1L, 999L);
    }

    @Test
    void getAllReports_delegatesToRepository() {
        List<AnalysisReport> reports = List.of(
                AnalysisReport.of(1L, "owner/repo1", "github", LocalDateTime.now(), List.of()),
                AnalysisReport.of(2L, "owner/repo2", "github", LocalDateTime.now(), List.of())
        );
        when(repository.findAll()).thenReturn(reports);

        List<AnalysisReport> result = service.getAllReports();

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }
}
