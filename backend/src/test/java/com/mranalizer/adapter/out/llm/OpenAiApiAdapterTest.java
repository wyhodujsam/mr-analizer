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

class OpenAiApiAdapterTest {

    private MockWebServer server;
    private OpenAiApiAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        adapter = new OpenAiApiAdapter(
                server.url("/").toString(),
                "test-key",
                "gpt-4o",
                0.1,
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
              "choices": [{"message": {"content": "{\\"scoreAdjustment\\": 0.15, \\"comment\\": \\"good PR\\"}"}}],
              "usage": {"prompt_tokens": 500, "completion_tokens": 100}
            }
            """;
        server.enqueue(new MockResponse().setBody(responseBody).setHeader("Content-Type", "application/json"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(0.15, result.scoreAdjustment(), 0.01);
        assertEquals("good PR", result.comment());
        assertEquals("openai-api", result.provider());
        assertEquals(500, result.cost().inputTokens());
        assertEquals(100, result.cost().outputTokens());
    }

    @Test
    void verifiesRequestFormat() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"scoreAdjustment\\\": 0.0}\"}}]}")
                .setHeader("Content-Type", "application/json"));

        adapter.analyze(testMr());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/v1/chat/completions"));
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"gpt-4o\""));
        assertTrue(body.contains("\"temperature\":0.1"));
    }

    @Test
    void authError_returns401() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_api_key\"}"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(0.0, result.scoreAdjustment());
        assertTrue(result.comment().contains("auth"));
    }

    @Test
    void rateLimitError_returns429() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));

        LlmAssessment result = adapter.analyze(testMr());

        assertEquals(0.0, result.scoreAdjustment());
        assertTrue(result.comment().contains("rate limit"));
    }

    @Test
    void providerName() {
        assertEquals("openai-api", adapter.getProviderName());
    }
}
