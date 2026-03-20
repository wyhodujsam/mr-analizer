package com.mranalizer.domain.port.in;

import com.mranalizer.domain.model.SavedRepository;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: manage saved repositories (add, remove, list).
 */
public interface ManageReposUseCase {

    List<SavedRepository> getAll();

    SavedRepository add(String projectSlug, String provider);

    void delete(Long id);

    Optional<SavedRepository> findBySlug(String projectSlug);
}
