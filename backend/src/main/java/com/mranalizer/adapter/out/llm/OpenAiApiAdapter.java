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
 * LLM adapter for OpenAI-compatible APIs (OpenAI, OpenRouter, LiteLLM, vLLM, Ollama, Azure OpenAI).
 * POST /v1/chat/completions
 */
@Component
@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "openai-api")
public class OpenAiApiAdapter implements LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(OpenAiApiAdapter.class);
    private static final String PROVIDER = "openai-api";

    private final WebClient webClient;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final int timeoutSeconds;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser responseParser;
    private final ObjectMapper objectMapper;

    public OpenAiApiAdapter(
            @Value("${mr-analizer.llm.openai-api.url:https://api.openai.com}") String apiUrl,
            @Value("${mr-analizer.llm.openai-api.key:}") String apiKey,
            @Value("${mr-analizer.llm.openai-api.model:gpt-4o}") String model,
            @Value("${mr-analizer.llm.openai-api.temperature:0.1}") double temperature,
            @Value("${mr-analizer.llm.openai-api.max-tokens:2000}") int maxTokens,
            @Value("${mr-analizer.llm.openai-api.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.promptBuilder = new PromptBuilder();
        this.responseParser = new LlmResponseParser(objectMapper);

        log.info("OpenAI API adapter initialized: url={}, model={}", apiUrl, model);
    }

    @Override
    public LlmAssessment analyze(MergeRequest mr) {
        String prompt = promptBuilder.build(null, mr);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        try {
            long start = System.currentTimeMillis();

            String responseJson = webClient.post()
                    .uri("/v1/chat/completions")
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
            String content = root.at("/choices/0/message/content").asText("");

            LlmAssessment base = responseParser.parseJsonResponse(content, PROVIDER);
            LlmCost cost = parseUsage(root, durationMs);

            return new LlmAssessment(base.scoreAdjustment(), base.comment(), PROVIDER,
                    base.overallAutomatability(), base.categories(), base.humanOversightRequired(),
                    base.whyLlmFriendly(), base.summaryTable(), cost);

        } catch (WebClientResponseException e) {
            log.warn("OpenAI API error {}: {}", e.getStatusCode(), e.getMessage());
            String msg = e.getStatusCode().value() == 401 ? "LLM auth error" :
                    e.getStatusCode().value() == 429 ? "LLM rate limit exceeded" :
                    "LLM error: HTTP " + e.getStatusCode().value();
            return new LlmAssessment(0.0, msg, PROVIDER);
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("Timeout")
                    ? "LLM timeout" : "LLM error: " + e.getMessage();
            log.warn("OpenAI API error for MR '{}': {}", mr.getTitle(), msg);
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

        int input = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt(0) : 0;
        int output = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt(0) : 0;
        return new LlmCost(input, output, 0, 0, 0.0, (int) durationMs);
    }
}
