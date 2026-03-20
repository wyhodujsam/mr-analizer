package com.mranalizer.adapter.out.persistence;

import com.mranalizer.domain.model.SavedRepository;
import com.mranalizer.domain.port.out.SavedRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("test")
public class InMemorySavedRepositoryAdapter implements SavedRepositoryPort {

    private final ConcurrentHashMap<Long, SavedRepository> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public SavedRepository save(SavedRepository repo) {
        Long id = (repo.getId() != null) ? repo.getId() : idSequence.getAndIncrement();
        SavedRepository toStore = SavedRepository.builder()
                .id(id)
                .projectSlug(repo.getProjectSlug())
                .provider(repo.getProvider())
                .addedAt(repo.getAddedAt())
                .lastAnalyzedAt(repo.getLastAnalyzedAt())
                .build();
        store.put(id, toStore);
        return toStore;
    }

    @Override
    public List<SavedRepository> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<SavedRepository> findByProjectSlug(String slug) {
        return store.values().stream()
                .filter(r -> r.getProjectSlug().equals(slug))
                .findFirst();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    /** Removes all stored repositories. Used for test isolation between scenarios. */
    public void clear() {
        store.clear();
    }
}
