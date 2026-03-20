package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.SavedRepositoryEntity;
import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.domain.port.out.SavedRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!test")
public class JpaSavedRepositoryAdapter implements SavedRepositoryPort {

    private final SpringDataSavedRepoRepository springRepo;

    public JpaSavedRepositoryAdapter(SpringDataSavedRepoRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public SavedRepository save(SavedRepository repo) {
        SavedRepositoryEntity entity = toEntity(repo);
        SavedRepositoryEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<SavedRepository> findAll() {
        return springRepo.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedRepository> findByProjectSlug(String slug) {
        return springRepo.findByProjectSlug(slug).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        springRepo.deleteById(id);
    }

    private SavedRepositoryEntity toEntity(SavedRepository repo) {
        SavedRepositoryEntity entity = new SavedRepositoryEntity();
        entity.setId(repo.getId());
        entity.setProjectSlug(repo.getProjectSlug());
        entity.setProvider(repo.getProvider());
        entity.setAddedAt(repo.getAddedAt());
        entity.setLastAnalyzedAt(repo.getLastAnalyzedAt());
        return entity;
    }

    private SavedRepository toDomain(SavedRepositoryEntity entity) {
        return SavedRepository.builder()
                .id(entity.getId())
                .projectSlug(entity.getProjectSlug())
                .provider(entity.getProvider())
                .addedAt(entity.getAddedAt())
                .lastAnalyzedAt(entity.getLastAnalyzedAt())
                .build();
    }
}
