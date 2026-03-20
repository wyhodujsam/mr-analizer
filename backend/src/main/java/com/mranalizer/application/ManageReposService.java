package com.mranalizer.application;

import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.SavedRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ManageReposService implements ManageReposUseCase {

    private final SavedRepositoryPort savedRepositoryPort;

    public ManageReposService(SavedRepositoryPort savedRepositoryPort) {
        this.savedRepositoryPort = savedRepositoryPort;
    }

    @Override
    public List<SavedRepository> getAll() {
        return savedRepositoryPort.findAll();
    }

    @Override
    public SavedRepository add(String projectSlug, String provider) {
        Optional<SavedRepository> existing = savedRepositoryPort.findByProjectSlug(projectSlug);
        if (existing.isPresent()) {
            return existing.get();
        }
        SavedRepository repo = SavedRepository.builder()
                .projectSlug(projectSlug)
                .provider(provider)
                .addedAt(LocalDateTime.now())
                .build();
        return savedRepositoryPort.save(repo);
    }

    @Override
    public void delete(Long id) {
        savedRepositoryPort.deleteById(id);
    }

    @Override
    public Optional<SavedRepository> findBySlug(String projectSlug) {
        return savedRepositoryPort.findByProjectSlug(projectSlug);
    }
}
