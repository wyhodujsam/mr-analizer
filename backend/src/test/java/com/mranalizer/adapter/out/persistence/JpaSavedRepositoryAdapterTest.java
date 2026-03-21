package com.mranalizer.adapter.out.persistence;

import com.mranalizer.domain.model.SavedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaSavedRepositoryAdapterTest {

    @Autowired
    private SpringDataSavedRepoRepository springRepo;

    private JpaSavedRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaSavedRepositoryAdapter(springRepo);
    }

    private SavedRepository buildRepo(String slug, String provider) {
        return SavedRepository.builder()
                .projectSlug(slug)
                .provider(provider)
                .addedAt(LocalDateTime.of(2026, 3, 15, 10, 0))
                .lastAnalyzedAt(LocalDateTime.of(2026, 3, 20, 14, 30))
                .build();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void saveAndFindAll_roundTrip() {
        SavedRepository saved = adapter.save(buildRepo("owner/alpha", "github"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProjectSlug()).isEqualTo("owner/alpha");
        assertThat(saved.getProvider()).isEqualTo("github");
        assertThat(saved.getAddedAt()).isEqualTo(LocalDateTime.of(2026, 3, 15, 10, 0));
        assertThat(saved.getLastAnalyzedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 14, 30));

        List<SavedRepository> all = adapter.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getProjectSlug()).isEqualTo("owner/alpha");
    }

    @Test
    void findAll_returnsMultiple() {
        adapter.save(buildRepo("owner/alpha", "github"));
        adapter.save(buildRepo("owner/beta", "gitlab"));
        adapter.save(buildRepo("owner/gamma", "github"));

        List<SavedRepository> all = adapter.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    void findByProjectSlug_found() {
        adapter.save(buildRepo("owner/alpha", "github"));
        adapter.save(buildRepo("owner/beta", "gitlab"));

        Optional<SavedRepository> found = adapter.findByProjectSlug("owner/beta");
        assertThat(found).isPresent();
        assertThat(found.get().getProjectSlug()).isEqualTo("owner/beta");
        assertThat(found.get().getProvider()).isEqualTo("gitlab");
    }

    @Test
    void findByProjectSlug_notFound() {
        adapter.save(buildRepo("owner/alpha", "github"));

        Optional<SavedRepository> found = adapter.findByProjectSlug("owner/nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteById_removesRepo() {
        SavedRepository saved = adapter.save(buildRepo("owner/alpha", "github"));
        assertThat(adapter.findAll()).hasSize(1);

        adapter.deleteById(saved.getId());
        assertThat(adapter.findAll()).isEmpty();
    }

    @Test
    void save_withNullLastAnalyzedAt() {
        SavedRepository repo = SavedRepository.builder()
                .projectSlug("owner/new-repo")
                .provider("github")
                .addedAt(LocalDateTime.of(2026, 3, 21, 8, 0))
                .build();

        SavedRepository saved = adapter.save(repo);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLastAnalyzedAt()).isNull();

        Optional<SavedRepository> found = adapter.findByProjectSlug("owner/new-repo");
        assertThat(found).isPresent();
        assertThat(found.get().getLastAnalyzedAt()).isNull();
    }

    @Test
    void save_updateExisting() {
        SavedRepository saved = adapter.save(buildRepo("owner/alpha", "github"));
        Long id = saved.getId();

        SavedRepository updated = SavedRepository.builder()
                .id(id)
                .projectSlug("owner/alpha")
                .provider("github")
                .addedAt(saved.getAddedAt())
                .lastAnalyzedAt(LocalDateTime.of(2026, 3, 21, 16, 0))
                .build();

        SavedRepository result = adapter.save(updated);
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getLastAnalyzedAt()).isEqualTo(LocalDateTime.of(2026, 3, 21, 16, 0));

        // Still only one entry
        assertThat(adapter.findAll()).hasSize(1);
    }

    @Test
    void deleteById_nonExistent_doesNotThrow() {
        // Should not throw when deleting non-existent ID
        adapter.deleteById(99999L);
        assertThat(adapter.findAll()).isEmpty();
    }
}
