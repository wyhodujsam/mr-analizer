package com.mranalizer.application;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrowseMrServiceTest {

    @Mock
    private MergeRequestProvider provider;

    @Mock
    private ManageReposUseCase manageReposUseCase;

    @InjectMocks
    private BrowseMrService service;

    @Test
    void browse_delegatesToProvider() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(50)
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of());
        when(provider.getProviderName()).thenReturn("github");

        service.browse(criteria);

        verify(provider).fetchMergeRequests(criteria);
    }

    @Test
    void browse_autoSavesRepo() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(50)
                .build();

        when(provider.fetchMergeRequests(any())).thenReturn(List.of());
        when(provider.getProviderName()).thenReturn("github");

        service.browse(criteria);

        verify(manageReposUseCase).add("owner/repo", "github");
    }

    @Test
    void browse_returnsProviderResults() {
        FetchCriteria criteria = FetchCriteria.builder()
                .projectSlug("owner/repo")
                .state("merged")
                .limit(50)
                .build();

        List<MergeRequest> mrs = List.of(
                buildMr(1L, "PR 1"),
                buildMr(2L, "PR 2"),
                buildMr(3L, "PR 3")
        );

        when(provider.fetchMergeRequests(any())).thenReturn(mrs);
        when(provider.getProviderName()).thenReturn("github");

        List<MergeRequest> result = service.browse(criteria);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("PR 1");
        assertThat(result.get(2).getTitle()).isEqualTo("PR 3");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MergeRequest buildMr(Long id, String title) {
        return MergeRequest.builder()
                .id(id)
                .externalId(String.valueOf(id))
                .title(title)
                .description("Description for " + title)
                .author("testauthor")
                .sourceBranch("feature/test")
                .targetBranch("main")
                .state("merged")
                .createdAt(LocalDateTime.now().minusDays(1))
                .mergedAt(LocalDateTime.now())
                .labels(List.of())
                .changedFiles(List.of(
                        new ChangedFile("src/File.java", 10, 3, "modified")
                ))
                .diffStats(new DiffStats(10, 3, 1))
                .hasTests(true)
                .ciPassed(true)
                .approvalsCount(1)
                .commentsCount(0)
                .provider("github")
                .url("https://github.com/owner/repo/pull/" + id)
                .projectSlug("owner/repo")
                .build();
    }
}
