package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.LlmCost;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.scoring.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LLM adapter for Anthropic Messages API.
 * POST /v1/messages with x-api-key header and anthropic-version.
 */
@Component
@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "anthropic-api")
public class AnthropicApiAdapter implements LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(AnthropicApiAdapter.class);
    private static final String PROVIDER = "anthropic-api";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser responseParser;
    private final ObjectMapper objectMapper;

    public AnthropicApiAdapter(
            @Value("${mr-analizer.llm.anthropic-api.url:https://api.anthropic.com}") String apiUrl,
            @Value("${mr-analizer.llm.anthropic-api.key:}") String apiKey,
            @Value("${mr-analizer.llm.anthropic-api.model:claude-sonnet-4-20250514}") String model,
            @Value("${mr-analizer.llm.anthropic-api.max-tokens:2000}") int maxTokens,
            @Value("${mr-analizer.llm.anthropic-api.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.promptBuilder = new PromptBuilder();
        this.responseParser = new LlmResponseParser(objectMapper);

        log.info("Anthropic API adapter initialized: url={}, model={}", apiUrl, model);
    }

    @Override
    public LlmAssessment analyze(MergeRequest mr) {
        String prompt = promptBuilder.build(null, mr);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", maxTokens
        );

        try {
            long start = System.currentTimeMillis();

            String responseJson = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long durationMs = System.currentTimeMillis() - start;

            if (responseJson == null) {
                return new LlmAssessment(0.0, "LLM error: empty response", PROVIDER);
            }

            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.at("/content/0/text").asText("");

            LlmAssessment base = responseParser.parseJsonResponse(content, PROVIDER);
            LlmCost cost = parseUsage(root, durationMs);

            return new LlmAssessment(base.scoreAdjustment(), base.comment(), PROVIDER,
                    base.overallAutomatability(), base.categories(), base.humanOversightRequired(),
                    base.whyLlmFriendly(), base.summaryTable(), cost);

        } catch (WebClientResponseException e) {
            log.warn("Anthropic API error {}: {}", e.getStatusCode(), e.getMessage());
            String msg = e.getStatusCode().value() == 401 ? "LLM auth error" :
                    e.getStatusCode().value() == 429 ? "LLM rate limit exceeded" :
                    "LLM error: HTTP " + e.getStatusCode().value();
            return new LlmAssessment(0.0, msg, PROVIDER);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("Timeout")
                    ? "LLM timeout" : "LLM error: " + e.getMessage();
            log.warn("Anthropic API error for MR '{}': {}", mr.getTitle(), msg);
            return new LlmAssessment(0.0, msg, PROVIDER);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER;
    }

    private LlmCost parseUsage(JsonNode root, long durationMs) {
        JsonNode usage = root.get("usage");
        if (usage == null) return LlmCost.empty();

        int input = usage.has("input_tokens") ? usage.get("input_tokens").asInt(0) : 0;
        int output = usage.has("output_tokens") ? usage.get("output_tokens").asInt(0) : 0;
        int cacheRead = usage.has("cache_read_input_tokens") ? usage.get("cache_read_input_tokens").asInt(0) : 0;
        int cacheCreation = usage.has("cache_creation_input_tokens") ? usage.get("cache_creation_input_tokens").asInt(0) : 0;
        return new LlmCost(input, output, cacheRead, cacheCreation, 0.0, (int) durationMs);
    }
}
