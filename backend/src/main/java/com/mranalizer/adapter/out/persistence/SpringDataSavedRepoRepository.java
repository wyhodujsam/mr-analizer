package com.mranalizer.adapter.out.persistence;

import com.mranalizer.adapter.out.persistence.entity.SavedRepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataSavedRepoRepository extends JpaRepository<SavedRepositoryEntity, Long> {

    Optional<SavedRepositoryEntity> findByProjectSlug(String slug);
}
