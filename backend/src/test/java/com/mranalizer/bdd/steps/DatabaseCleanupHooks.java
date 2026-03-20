package com.mranalizer.bdd.steps;

import com.mranalizer.adapter.out.persistence.InMemoryAnalysisResultRepository;
import com.mranalizer.adapter.out.persistence.InMemorySavedRepositoryAdapter;
import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cucumber hook that clears in-memory repositories before each scenario to ensure test isolation.
 * Required because cache logic (findByProjectSlug) would otherwise return stale data
 * from previous scenarios.
 */
public class DatabaseCleanupHooks {

    @Autowired
    private InMemoryAnalysisResultRepository analysisResultRepository;

    @Autowired
    private InMemorySavedRepositoryAdapter savedRepositoryAdapter;

    @Before(order = 0) // Run before other @Before hooks
    public void cleanDatabase() {
        analysisResultRepository.clear();
        savedRepositoryAdapter.clear();
    }
}
