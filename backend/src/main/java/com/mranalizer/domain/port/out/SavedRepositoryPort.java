package com.mranalizer.domain.port.out;

import com.mranalizer.domain.model.SavedRepository;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence abstraction for {@link SavedRepository}.
 */
public interface SavedRepositoryPort {

    SavedRepository save(SavedRepository repo);

    List<SavedRepository> findAll();

    Optional<SavedRepository> findByProjectSlug(String slug);

    void deleteById(Long id);
}
