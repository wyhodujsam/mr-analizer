package com.mranalizer.adapter.config;

import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringConfig;
import com.mranalizer.domain.scoring.ScoringEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "mr-analizer.rules.exclude.labels=wip,do-not-merge",
        "mr-analizer.rules.exclude.file-extensions-only=.md,.txt",
        "mr-analizer.rules.boost.description-keywords.words=refactor,fix",
        "mr-analizer.rules.boost.labels.values=ready,approved"
})
@ActiveProfiles("test")
class RulesConfigTest {

    @Autowired
    private List<Rule> rules;

    @Autowired
    private ScoringEngine scoringEngine;

    @Autowired
    private ScoringConfig scoringConfig;

    @Test
    void rulesAreConfigured() {
        assertFalse(rules.isEmpty());
        // With all optional lists populated, we expect:
        // 4 exclude rules (labels, minFiles, maxFiles, fileExtensions)
        // 4 boost rules (keywords, hasTests, changedFilesRange, labels)
        // 3 penalize rules (largeDiff, noDescription, touchesConfig)
        // = 11 total
        assertEquals(11, rules.size());
    }

    @Test
    void scoringEngineIsConfigured() {
        assertNotNull(scoringEngine);
    }

    @Test
    void scoringConfigHasDefaults() {
        assertEquals(0.5, scoringConfig.getBaseScore());
        assertEquals(0.7, scoringConfig.getAutomatableThreshold());
        assertEquals(0.4, scoringConfig.getMaybeThreshold());
    }
}
