package com.mranalizer.integration;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-github")
abstract class GitHubIntegrationTestBase {

    // Single shared server — lives for entire test suite. Never shutdown/restarted
    // so Spring's cached context keeps the correct port.
    static final MockWebServer githubApi;

    static {
        githubApi = new MockWebServer();
        try {
            githubApi.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void overrideGitHubUrl(DynamicPropertyRegistry registry) {
        registry.add("mr-analizer.github.api-url",
                () -> "http://localhost:" + githubApi.getPort());
    }

    protected static void setupDispatcher(Map<String, String> routes) {
        Map<Pattern, String> compiled = new ConcurrentHashMap<>();
        for (var entry : routes.entrySet()) {
            compiled.put(Pattern.compile(entry.getKey()), entry.getValue());
        }

        githubApi.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                for (var entry : compiled.entrySet()) {
                    if (entry.getKey().matcher(path).matches()) {
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody(loadFixture(entry.getValue()));
                    }
                }
                return new MockResponse().setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"message\":\"Not found: " + path + "\"}");
            }
        });
    }

    protected static void setupErrorDispatcher(int statusCode) {
        githubApi.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(statusCode)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"message\":\"Error " + statusCode + "\"}");
            }
        });
    }

    protected static String loadFixture(String name) {
        String path = "github-fixtures/" + name;
        try (InputStream is = GitHubIntegrationTestBase.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Fixture not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load fixture: " + path, e);
        }
    }
}
