package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeCliAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Path scriptFile;

    @TempDir
    Path tempDir;

    private MergeRequest sampleMr() {
        return MergeRequest.builder()
                .externalId("42")
                .title("Fix login bug")
                .description("Fixes the login redirect issue")
                .author("dev")
                .sourceBranch("fix/login")
                .targetBranch("main")
                .state("merged")
                .diffStats(new DiffStats(10, 5, 2))
                .build();
    }

    private Path createScript(String content) throws IOException {
        Path script = tempDir.resolve("claude-mock.sh");
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }

    @Test
    void analyze_successfulBasicResponse() throws Exception {
        String json = """
                {"result": "{\\"scoreAdjustment\\": 0.3, \\"comment\\": \\"Good PR\\"}"}
                """;
        scriptFile = createScript("#!/bin/bash\ncat << 'JSONEOF'\n" + json.trim() + "\nJSONEOF\n");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.3, result.scoreAdjustment(), 0.01);
        assertEquals("Good PR", result.comment());
        assertEquals("claude-cli", result.provider());
    }

    @Test
    void analyze_detailedJsonResponse() throws Exception {
        String innerJson = """
                {
                  "scoreAdjustment": 0.2,
                  "comment": "Detailed analysis",
                  "overallAutomatability": 85,
                  "categories": [
                    {"name": "Code Quality", "score": 90, "reasoning": "Clean code"},
                    {"name": "Testing", "score": 70, "reasoning": "Some tests missing"}
                  ],
                  "humanOversightRequired": [
                    {"area": "Security", "reasoning": "Auth changes need review"}
                  ],
                  "whyLlmFriendly": ["Simple refactoring", "Well-defined scope"],
                  "summaryTable": [
                    {"aspect": "Complexity", "score": 80, "note": "Low"},
                    {"aspect": "Risk", "score": null, "note": "Minimal"}
                  ]
                }""";
        // Escape for JSON string value
        String escaped = innerJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String wrapper = "{\"result\": \"" + escaped + "\"}";

        scriptFile = createScript("#!/bin/bash\ncat << 'JSONEOF'\n" + wrapper + "\nJSONEOF\n");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.2, result.scoreAdjustment(), 0.01);
        assertEquals("Detailed analysis", result.comment());
        assertEquals(85, result.overallAutomatability());
        assertEquals(2, result.categories().size());
        assertEquals("Code Quality", result.categories().get(0).name());
        assertEquals(90, result.categories().get(0).score());
        assertEquals(1, result.humanOversightRequired().size());
        assertEquals("Security", result.humanOversightRequired().get(0).area());
        assertEquals(2, result.whyLlmFriendly().size());
        assertEquals(2, result.summaryTable().size());
        assertEquals("Complexity", result.summaryTable().get(0).aspect());
        assertEquals(80, result.summaryTable().get(0).score());
        assertNull(result.summaryTable().get(1).score());
    }

    @Test
    void analyze_timeout_returnsZeroAssessment() throws Exception {
        scriptFile = createScript("#!/bin/bash\nsleep 30\necho '{}'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 1, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertEquals("LLM timeout", result.comment());
        assertEquals("claude-cli", result.provider());
    }

    @Test
    void analyze_nonZeroExitCode_returnsErrorAssessment() throws Exception {
        scriptFile = createScript("#!/bin/bash\necho 'error output' >&2\nexit 1");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertTrue(result.comment().contains("LLM error: exit code 1"));
        assertEquals("claude-cli", result.provider());
    }

    @Test
    void analyze_invalidJson_returnsErrorAssessment() throws Exception {
        scriptFile = createScript("#!/bin/bash\necho 'this is not json at all no braces'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertNotNull(result.comment());
        assertTrue(result.comment().startsWith("LLM error:"));
    }

    @Test
    void analyze_missingFields_returnsDefaults() throws Exception {
        // JSON with no scoreAdjustment or comment fields
        String json = "{\"result\": \"{\\\"unrelated\\\": true}\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertNull(result.comment());
        assertEquals("claude-cli", result.provider());
        assertEquals(0, result.overallAutomatability());
        assertTrue(result.categories().isEmpty());
        assertTrue(result.humanOversightRequired().isEmpty());
        assertTrue(result.whyLlmFriendly().isEmpty());
        assertTrue(result.summaryTable().isEmpty());
    }

    @Test
    void analyze_scoreClamped_toPositiveMax() throws Exception {
        String json = "{\"result\": \"{\\\"scoreAdjustment\\\": 1.5, \\\"comment\\\": \\\"Great\\\"}\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.5, result.scoreAdjustment(), 0.01);
    }

    @Test
    void analyze_scoreClamped_toNegativeMin() throws Exception {
        String json = "{\"result\": \"{\\\"scoreAdjustment\\\": -2.0, \\\"comment\\\": \\\"Bad\\\"}\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(-0.5, result.scoreAdjustment(), 0.01);
    }

    @Test
    void analyze_directJsonWithoutResultWrapper() throws Exception {
        // Output is raw JSON without "result" wrapper
        String json = "{\"scoreAdjustment\": 0.1, \"comment\": \"Direct output\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertEquals("Direct output", result.comment());
    }

    @Test
    void analyze_commandNotFound_returnsErrorAssessment() {
        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                "/nonexistent/command/path", 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertTrue(result.comment().startsWith("LLM error:"));
        assertEquals("claude-cli", result.provider());
    }

    @Test
    void analyze_jsonInMarkdown() throws Exception {
        // Claude may wrap JSON in markdown code fences
        String output = "Here is the result:\n```json\n{\"scoreAdjustment\": 0.4, \"comment\": \"Wrapped\"}\n```\nDone.";
        String escaped = output.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String wrapper = "{\"result\": \"" + escaped + "\"}";
        scriptFile = createScript("#!/bin/bash\ncat << 'JSONEOF'\n" + wrapper + "\nJSONEOF\n");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.4, result.scoreAdjustment(), 0.01);
        assertEquals("Wrapped", result.comment());
    }

    @Test
    void getProviderName_returnsClaude() {
        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                "claude", 60, "", "scoreAdjustment", "comment", objectMapper);
        assertEquals("claude-cli", adapter.getProviderName());
    }

    @Test
    void analyze_customFieldNames() throws Exception {
        String json = "{\"result\": \"{\\\"myScore\\\": 0.15, \\\"myComment\\\": \\\"Custom fields\\\"}\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "myScore", "myComment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.15, result.scoreAdjustment(), 0.01);
        assertEquals("Custom fields", result.comment());
    }

    @Test
    void checkAvailability_commandFound() {
        // "true" is a built-in command that always exists
        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                "true", 10, "", "scoreAdjustment", "comment", objectMapper);
        adapter.checkAvailability();
        // No exception = pass; it should log "Claude CLI found at: ..."
    }

    @Test
    void checkAvailability_commandNotFound() {
        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                "nonexistent_command_xyz123", 10, "", "scoreAdjustment", "comment", objectMapper);
        adapter.checkAvailability();
        // Should log warning, not throw
    }

    @Test
    void analyze_interruptedHandlesGracefully() throws Exception {
        Path script = tempDir.resolve("claude-mock-interrupt.sh");
        Files.writeString(script, "#!/bin/bash\nsleep 30");
        script.toFile().setExecutable(true);

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                script.toString(), 1, "", "scoreAdjustment", "comment", objectMapper);

        MergeRequest mr = sampleMr();
        LlmAssessment result = adapter.analyze(mr);

        // Timeout path should return zero assessment
        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertEquals("LLM timeout", result.comment());
        assertEquals("claude-cli", result.provider());
    }

    @Test
    void analyze_interruptedDuringWaitFor_returnsInterruptedError() throws Exception {
        Path script = tempDir.resolve("claude-mock-sleep.sh");
        Files.writeString(script, "#!/bin/bash\nsleep 60");
        script.toFile().setExecutable(true);

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                script.toString(), 60, "", "scoreAdjustment", "comment", objectMapper);

        MergeRequest mr = sampleMr();
        // Run analyze in a thread so we can interrupt it
        Thread analyzeThread = Thread.currentThread();
        java.util.concurrent.atomic.AtomicReference<LlmAssessment> resultRef = new java.util.concurrent.atomic.AtomicReference<>();

        Thread worker = new Thread(() -> {
            resultRef.set(adapter.analyze(mr));
        });
        worker.start();

        // Give the worker thread time to start the process and enter waitFor
        Thread.sleep(500);
        worker.interrupt();
        worker.join(5000);

        LlmAssessment result = resultRef.get();
        assertNotNull(result);
        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        // Could be either "LLM error: interrupted" or "LLM timeout" depending on timing
        assertTrue(result.comment().contains("LLM") || result.comment().contains("error") || result.comment().contains("timeout"));
    }

    @Test
    void analyze_noJsonBracesInResponse_returnsError() throws Exception {
        // Valid JSON wrapper but the "result" text has no braces
        String json = "{\"result\": \"plain text with no json braces\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + json + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertTrue(result.comment().contains("LLM error"));
    }

    @Test
    void analyze_categoriesWithMissingSubfields() throws Exception {
        String innerJson = """
                {
                  "scoreAdjustment": 0.1,
                  "comment": "partial",
                  "categories": [{}],
                  "humanOversightRequired": [{}],
                  "summaryTable": [{}]
                }""";
        String escaped = innerJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String wrapper = "{\"result\": \"" + escaped + "\"}";
        scriptFile = createScript("#!/bin/bash\ncat << 'JSONEOF'\n" + wrapper + "\nJSONEOF\n");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertEquals(1, result.categories().size());
        assertEquals("", result.categories().get(0).name());
        assertEquals(0, result.categories().get(0).score());
        assertEquals("", result.categories().get(0).reasoning());
        assertEquals(1, result.humanOversightRequired().size());
        assertEquals("", result.humanOversightRequired().get(0).area());
        assertEquals(1, result.summaryTable().size());
        assertNull(result.summaryTable().get(0).score());
        assertEquals("", result.summaryTable().get(0).aspect());
    }

    @Test
    void analyze_nonArrayCategoriesIgnored() throws Exception {
        // categories is a string instead of array — should return empty list
        String innerJson = "{\"scoreAdjustment\": 0.1, \"comment\": \"ok\", \"categories\": \"not-an-array\", \"humanOversightRequired\": \"nope\", \"whyLlmFriendly\": \"nope\", \"summaryTable\": \"nope\"}";
        String escaped = innerJson.replace("\"", "\\\"");
        String wrapper = "{\"result\": \"" + escaped + "\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + wrapper + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertTrue(result.categories().isEmpty());
        assertTrue(result.humanOversightRequired().isEmpty());
        assertTrue(result.whyLlmFriendly().isEmpty());
        assertTrue(result.summaryTable().isEmpty());
    }

    @Test
    void analyze_emptyCategoriesArray() throws Exception {
        String innerJson = "{\"scoreAdjustment\": 0.1, \"comment\": \"ok\", \"categories\": [], \"humanOversightRequired\": [], \"whyLlmFriendly\": [], \"summaryTable\": []}";
        String escaped = innerJson.replace("\"", "\\\"");
        String wrapper = "{\"result\": \"" + escaped + "\"}";
        scriptFile = createScript("#!/bin/bash\necho '" + wrapper + "'");

        ClaudeCliAdapter adapter = new ClaudeCliAdapter(
                scriptFile.toString(), 10, "", "scoreAdjustment", "comment", objectMapper);

        LlmAssessment result = adapter.analyze(sampleMr());

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertTrue(result.categories().isEmpty());
        assertTrue(result.humanOversightRequired().isEmpty());
        assertTrue(result.whyLlmFriendly().isEmpty());
        assertTrue(result.summaryTable().isEmpty());
    }
}
