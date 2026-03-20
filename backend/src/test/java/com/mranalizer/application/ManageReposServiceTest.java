package com.mranalizer.application;

import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.domain.port.out.SavedRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageReposServiceTest {

    @Mock
    private SavedRepositoryPort savedRepositoryPort;

    @InjectMocks
    private ManageReposService service;

    @Test
    void add_savesNewRepo() {
        when(savedRepositoryPort.findByProjectSlug("owner/repo")).thenReturn(Optional.empty());
        SavedRepository saved = SavedRepository.builder()
                .id(1L)
                .projectSlug("owner/repo")
                .provider("github")
                .addedAt(LocalDateTime.now())
                .build();
        when(savedRepositoryPort.save(any())).thenReturn(saved);

        SavedRepository result = service.add("owner/repo", "github");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectSlug()).isEqualTo("owner/repo");
        assertThat(result.getProvider()).isEqualTo("github");

        ArgumentCaptor<SavedRepository> captor = ArgumentCaptor.forClass(SavedRepository.class);
        verify(savedRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getProjectSlug()).isEqualTo("owner/repo");
    }

    @Test
    void add_existingSlug_returnsExisting() {
        SavedRepository existing = SavedRepository.builder()
                .id(5L)
                .projectSlug("owner/repo")
                .provider("github")
                .addedAt(LocalDateTime.now().minusDays(7))
                .build();
        when(savedRepositoryPort.findByProjectSlug("owner/repo")).thenReturn(Optional.of(existing));

        SavedRepository result = service.add("owner/repo", "github");

        assertThat(result.getId()).isEqualTo(5L);
        verify(savedRepositoryPort, never()).save(any());
    }

    @Test
    void getAll_delegatesToPort() {
        List<SavedRepository> repos = List.of(
                SavedRepository.builder().id(1L).projectSlug("a/b").provider("github").build(),
                SavedRepository.builder().id(2L).projectSlug("c/d").provider("gitlab").build()
        );
        when(savedRepositoryPort.findAll()).thenReturn(repos);

        List<SavedRepository> result = service.getAll();

        assertThat(result).hasSize(2);
        verify(savedRepositoryPort).findAll();
    }

    @Test
    void delete_delegatesToPort() {
        service.delete(42L);

        verify(savedRepositoryPort).deleteById(42L);
    }

    @Test
    void findBySlug_delegatesToPort() {
        SavedRepository repo = SavedRepository.builder()
                .id(1L)
                .projectSlug("owner/repo")
                .provider("github")
                .build();
        when(savedRepositoryPort.findByProjectSlug("owner/repo")).thenReturn(Optional.of(repo));

        Optional<SavedRepository> result = service.findBySlug("owner/repo");

        assertThat(result).isPresent();
        assertThat(result.get().getProjectSlug()).isEqualTo("owner/repo");
        verify(savedRepositoryPort).findByProjectSlug("owner/repo");
    }
}
