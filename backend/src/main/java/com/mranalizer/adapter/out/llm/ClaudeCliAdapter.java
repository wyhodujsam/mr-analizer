package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.scoring.PromptBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "claude-cli")
public class ClaudeCliAdapter implements LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliAdapter.class);
    private static final String PROVIDER = "claude-cli";

    private final String command;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser responseParser;
    private final String promptTemplate;

    public ClaudeCliAdapter(
            @Value("${mr-analizer.llm.claude-cli.command:claude}") String command,
            @Value("${mr-analizer.llm.claude-cli.timeout-seconds:60}") int timeoutSeconds,
            @Value("${mr-analizer.llm.claude-cli.prompt-template:}") String promptTemplate,
            @Value("${mr-analizer.llm.claude-cli.response-score-field:scoreAdjustment}") String responseScoreField,
            @Value("${mr-analizer.llm.claude-cli.response-comment-field:comment}") String responseCommentField,
            ObjectMapper objectMapper) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
        this.promptTemplate = promptTemplate;
        this.objectMapper = objectMapper;
        this.promptBuilder = new PromptBuilder();
        this.responseParser = new LlmResponseParser(objectMapper, responseScoreField, responseCommentField);
    }

    @PostConstruct
    void checkAvailability() {
        try {
            Process process = new ProcessBuilder("which", command)
                    .redirectErrorStream(true)
                    .start();
            boolean found = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
            if (found) {
                log.info("Claude CLI found at: {}", new String(process.getInputStream().readAllBytes()).trim());
            } else {
                log.warn("Claude CLI command '{}' not found in PATH.", command);
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Could not verify Claude CLI availability: {}", e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    @Override
    public LlmAssessment analyze(MergeRequest mr) {
        String prompt = promptBuilder.build(promptTemplate, mr);

        try {
            Process process = new ProcessBuilder(command, "-p", prompt, "--output-format", "json")
                    .redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")))
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Claude CLI timed out after {}s for MR '{}'", timeoutSeconds, mr.getTitle());
                return new LlmAssessment(0.0, "LLM timeout", PROVIDER);
            }

            String output = new String(process.getInputStream().readAllBytes());

            if (process.exitValue() != 0) {
                log.warn("Claude CLI exited with code {} for MR '{}': {}", process.exitValue(), mr.getTitle(), output);
                return new LlmAssessment(0.0, "LLM error: exit code " + process.exitValue(), PROVIDER);
            }

            return parseResponse(output);

        } catch (IOException e) {
            log.error("Failed to run Claude CLI for MR '{}': {}", mr.getTitle(), e.getMessage());
            return new LlmAssessment(0.0, "LLM error: " + e.getMessage(), PROVIDER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LlmAssessment(0.0, "LLM error: interrupted", PROVIDER);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER;
    }

    private LlmAssessment parseResponse(String rawOutput) {
        try {
            JsonNode root = objectMapper.readTree(rawOutput);

            // Claude --output-format json wraps result in {"result": "..."}
            String text = rawOutput;
            if (root.has("result")) {
                text = root.get("result").asText();
            }

            // Delegate to shared parser
            LlmAssessment base = responseParser.parseJsonResponse(text, PROVIDER);

            // CLI-specific: cost info is in root JSON, not in content
            LlmCost cost = parseCost(root);

            return new LlmAssessment(base.scoreAdjustment(), base.comment(), PROVIDER,
                    base.overallAutomatability(), base.categories(), base.humanOversightRequired(),
                    base.whyLlmFriendly(), base.summaryTable(), cost);

        } catch (Exception e) {
            log.warn("Failed to parse Claude CLI response: {}", e.getMessage());
            return new LlmAssessment(0.0, "LLM error: " + e.getMessage(), PROVIDER);
        }
    }

    private LlmCost parseCost(JsonNode root) {
        try {
            JsonNode usage = root.get("usage");
            if (usage == null) return LlmCost.empty();

            int inputTokens = usage.has("input_tokens") ? usage.get("input_tokens").asInt(0) : 0;
            int outputTokens = usage.has("output_tokens") ? usage.get("output_tokens").asInt(0) : 0;
            int cacheRead = usage.has("cache_read_input_tokens") ? usage.get("cache_read_input_tokens").asInt(0) : 0;
            int cacheCreation = usage.has("cache_creation_input_tokens") ? usage.get("cache_creation_input_tokens").asInt(0) : 0;
            double costUsd = root.has("total_cost_usd") ? root.get("total_cost_usd").asDouble(0.0) : 0.0;
            int durationMs = root.has("duration_ms") ? root.get("duration_ms").asInt(0) : 0;

            return new LlmCost(inputTokens, outputTokens, cacheRead, cacheCreation, costUsd, durationMs);
        } catch (Exception e) {
            log.warn("Failed to parse LLM cost: {}", e.getMessage());
            return LlmCost.empty();
        }
    }
}
