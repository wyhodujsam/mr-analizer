package com.mranalizer.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AnthropicApiAdapterTest {

    private MockWebServer server;
    private AnthropicApiAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        adapter = new AnthropicApiAdapter(
                server.url("/").toString(),
                "test-anthropic-key",
                "claude-sonnet-4-20250514",
                2000,
                10,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private MergeRequest testMr() {
        return MergeRequest.builder()
                .externalId("1").title("Test PR").author("alice").state("merged")
                .description("Fix bug").diffStats(new DiffStats(100, 20, 3))
                .build();
    }

    @Test
    void happyPath_returnsAssessment() throws Exception {
        String responseBody = """
            {
              "content": [{"type": "text", "text": "{\\"scoreAdjustment\\": 0.1, \\"comment\\": \\"doable\\"}"}],
              "usage": {"input_tokens": 400, "output_tokens": 80}
            }
            """;
        server.enqueue(new MockResponse().setBody(responseBody).setHeader("Content-Type", "application/json"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(0.1, result.scoreAdjustment(), 0.01);
        assertEquals("doable", result.comment());
        assertEquals("anthropic-api", result.provider());
        assertEquals(400, result.cost().inputTokens());
        assertEquals(80, result.cost().outputTokens());
    }

    @Test
    void verifiesAnthropicHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"scoreAdjustment\\\": 0.0}\"}]}")
                .setHeader("Content-Type", "application/json"));

        adapter.analyze(testMr());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/v1/messages"));
        assertEquals("test-anthropic-key", request.getHeader("x-api-key"));
        assertEquals("2023-06-01", request.getHeader("anthropic-version"));
        assertNull(request.getHeader("Authorization")); // Anthropic uses x-api-key, NOT Bearer
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"claude-sonnet-4-20250514\""));
    }

    @Test
    void authError_returns401() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_api_key\"}"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(0.0, result.scoreAdjustment());
        assertTrue(result.comment().contains("auth"));
    }

    @Test
    void cacheTokens_parsed() throws Exception {
        String responseBody = """
            {
              "content": [{"type": "text", "text": "{\\"scoreAdjustment\\": 0.05}"}],
              "usage": {"input_tokens": 300, "output_tokens": 50, "cache_read_input_tokens": 200, "cache_creation_input_tokens": 100}
            }
            """;
        server.enqueue(new MockResponse().setBody(responseBody).setHeader("Content-Type", "application/json"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(200, result.cost().cacheReadTokens());
        assertEquals(100, result.cost().cacheCreationTokens());
    }

    @Test
    void providerName() {
        assertEquals("anthropic-api", adapter.getProviderName());
    }
}
