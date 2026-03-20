package com.mranalizer.domain.scoring;

/**
 * Configuration holder for the scoring engine — plain Java, no framework dependencies.
 */
public class ScoringConfig {

    private final double baseScore;
    private final double automatableThreshold;
    private final double maybeThreshold;

    public ScoringConfig() {
        this(0.5, 0.7, 0.4);
    }

    public ScoringConfig(double baseScore, double automatableThreshold, double maybeThreshold) {
        this.baseScore = baseScore;
        this.automatableThreshold = automatableThreshold;
        this.maybeThreshold = maybeThreshold;
    }

    public double getBaseScore() {
        return baseScore;
    }

    public double getAutomatableThreshold() {
        return automatableThreshold;
    }

    public double getMaybeThreshold() {
        return maybeThreshold;
    }
}
