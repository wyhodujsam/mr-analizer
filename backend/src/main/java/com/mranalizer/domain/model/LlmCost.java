package com.mranalizer.domain.model;

public record LlmCost(
        int inputTokens,
        int outputTokens,
        int cacheReadTokens,
        int cacheCreationTokens,
        double costUsd,
        int durationMs
) {
    public static LlmCost empty() {
        return new LlmCost(0, 0, 0, 0, 0.0, 0);
    }

    public int totalTokens() {
        return inputTokens + outputTokens + cacheReadTokens + cacheCreationTokens;
    }
}
