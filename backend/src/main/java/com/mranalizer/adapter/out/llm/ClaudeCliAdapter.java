package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "claude-cli")
public class ClaudeCliAdapter implements LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCliAdapter.class);
    private static final String PROVIDER = "claude-cli";

    private final String command;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public ClaudeCliAdapter(
            @Value("${mr-analizer.llm.claude-cli.command:claude}") String command,
            @Value("${mr-analizer.llm.claude-cli.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
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
                log.warn("Claude CLI command '{}' not found in PATH. LLM analysis calls will likely fail.", command);
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Could not verify Claude CLI availability: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public LlmAssessment analyze(MergeRequest mr) {
        String prompt = buildPrompt(mr);

        try {
            Process process = new ProcessBuilder(command, "-p", prompt, "--output-format", "json")
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

    private String buildPrompt(MergeRequest mr) {
        List<String> labels = mr.getLabels();
        String labelsStr = (labels != null && !labels.isEmpty()) ? String.join(", ", labels) : "none";
        int additions = mr.getDiffStats() != null ? mr.getDiffStats().additions() : 0;
        int deletions = mr.getDiffStats() != null ? mr.getDiffStats().deletions() : 0;
        int filesChanged = mr.getDiffStats() != null ? mr.getDiffStats().changedFilesCount() : 0;

        return "Analyze this Pull Request for LLM automation potential. " +
                "Rate from -0.5 to +0.5 how suitable it is for automated execution. " +
                "Respond in JSON: {\"scoreAdjustment\": 0.1, \"comment\": \"explanation\"}\n\n" +
                "Title: " + mr.getTitle() + "\n" +
                "Description: " + (mr.getDescription() != null ? mr.getDescription() : "none") + "\n" +
                "Files changed: " + filesChanged + "\n" +
                "Additions: " + additions + "\n" +
                "Deletions: " + deletions + "\n" +
                "Has tests: " + mr.isHasTests() + "\n" +
                "Labels: " + labelsStr;
    }

    private LlmAssessment parseResponse(String rawOutput) {
        try {
            // Claude --output-format json wraps the result; extract the text content
            JsonNode root = objectMapper.readTree(rawOutput);

            // The JSON output format may contain a "result" field with the text
            String text = rawOutput;
            if (root.has("result")) {
                text = root.get("result").asText();
            }

            // Find JSON object in the text (Claude may wrap it in markdown)
            int braceStart = text.indexOf('{');
            int braceEnd = text.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                String jsonStr = text.substring(braceStart, braceEnd + 1);
                JsonNode parsed = objectMapper.readTree(jsonStr);

                double score = parsed.has("scoreAdjustment") ? parsed.get("scoreAdjustment").asDouble(0.0) : 0.0;
                String comment = parsed.has("comment") ? parsed.get("comment").asText() : null;

                // Clamp score to valid range
                score = Math.max(-0.5, Math.min(0.5, score));

                return new LlmAssessment(score, comment, PROVIDER);
            }

            log.warn("Could not find JSON in Claude CLI response: {}", rawOutput);
            return new LlmAssessment(0.0, "LLM error: no JSON in response", PROVIDER);

        } catch (Exception e) {
            log.warn("Failed to parse Claude CLI response: {}", e.getMessage());
            return new LlmAssessment(0.0, "LLM error: " + e.getMessage(), PROVIDER);
        }
    }
}
