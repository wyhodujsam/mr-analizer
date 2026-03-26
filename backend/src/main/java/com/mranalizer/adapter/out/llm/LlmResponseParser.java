package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared parser for LLM JSON responses. Used by all LLM adapters (CLI, OpenAI API, Anthropic API).
 * Extracts scoreAdjustment, comment, categories, humanOversight, whyLlmFriendly, summaryTable.
 */
public class LlmResponseParser {

    private static final Logger log = LoggerFactory.getLogger(LlmResponseParser.class);

    private final ObjectMapper objectMapper;
    private final String scoreField;
    private final String commentField;

    public LlmResponseParser(ObjectMapper objectMapper, String scoreField, String commentField) {
        this.objectMapper = objectMapper;
        this.scoreField = scoreField;
        this.commentField = commentField;
    }

    public LlmResponseParser(ObjectMapper objectMapper) {
        this(objectMapper, "scoreAdjustment", "comment");
    }

    /**
     * Parse raw text (may contain JSON embedded in markdown) into LlmAssessment.
     */
    public LlmAssessment parseJsonResponse(String rawText, String provider) {
        try {
            int braceStart = rawText.indexOf('{');
            int braceEnd = rawText.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                String jsonStr = rawText.substring(braceStart, braceEnd + 1);
                JsonNode parsed = objectMapper.readTree(jsonStr);

                double score = parsed.has(scoreField) ? parsed.get(scoreField).asDouble(0.0) : 0.0;
                String comment = parsed.has(commentField) ? parsed.get(commentField).asText() : null;

                score = Math.max(-0.5, Math.min(0.5, score));

                int overallAutomatability = parsed.has("overallAutomatability")
                        ? parsed.get("overallAutomatability").asInt(0) : 0;

                List<AnalysisCategory> categories = parseCategories(parsed);
                List<HumanOversightItem> humanOversight = parseHumanOversight(parsed);
                List<String> whyLlmFriendly = parseStringArray(parsed, "whyLlmFriendly");
                List<SummaryAspect> summaryTable = parseSummaryTable(parsed);

                return new LlmAssessment(score, comment, provider,
                        overallAutomatability, categories, humanOversight, whyLlmFriendly, summaryTable);
            }

            log.warn("No JSON found in LLM response for provider {}", provider);
            return new LlmAssessment(0.0, "LLM error: no JSON in response", provider);

        } catch (Exception e) {
            log.warn("Failed to parse LLM response for provider {}: {}", provider, e.getMessage());
            return new LlmAssessment(0.0, "LLM error: " + e.getMessage(), provider);
        }
    }

    public LlmCost buildCost(int inputTokens, int outputTokens, int cacheRead, int cacheCreation,
                              double costUsd, int durationMs) {
        return new LlmCost(inputTokens, outputTokens, cacheRead, cacheCreation, costUsd, durationMs);
    }

    private List<AnalysisCategory> parseCategories(JsonNode parsed) {
        if (!parsed.has("categories") || !parsed.get("categories").isArray()) return List.of();
        List<AnalysisCategory> result = new ArrayList<>();
        for (JsonNode node : parsed.get("categories")) {
            result.add(new AnalysisCategory(
                    node.has("name") ? node.get("name").asText() : "",
                    node.has("score") ? node.get("score").asInt(0) : 0,
                    node.has("reasoning") ? node.get("reasoning").asText() : ""));
        }
        return result;
    }

    private List<HumanOversightItem> parseHumanOversight(JsonNode parsed) {
        if (!parsed.has("humanOversightRequired") || !parsed.get("humanOversightRequired").isArray()) return List.of();
        List<HumanOversightItem> result = new ArrayList<>();
        for (JsonNode node : parsed.get("humanOversightRequired")) {
            result.add(new HumanOversightItem(
                    node.has("area") ? node.get("area").asText() : "",
                    node.has("reasoning") ? node.get("reasoning").asText() : ""));
        }
        return result;
    }

    private List<String> parseStringArray(JsonNode parsed, String field) {
        if (!parsed.has(field) || !parsed.get(field).isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode node : parsed.get(field)) {
            result.add(node.asText());
        }
        return result;
    }

    private List<SummaryAspect> parseSummaryTable(JsonNode parsed) {
        if (!parsed.has("summaryTable") || !parsed.get("summaryTable").isArray()) return List.of();
        List<SummaryAspect> result = new ArrayList<>();
        for (JsonNode node : parsed.get("summaryTable")) {
            Integer score = node.has("score") && !node.get("score").isNull() ? node.get("score").asInt() : null;
            result.add(new SummaryAspect(
                    node.has("aspect") ? node.get("aspect").asText() : "",
                    score,
                    node.has("note") ? node.get("note").asText() : ""));
        }
        return result;
    }
}
