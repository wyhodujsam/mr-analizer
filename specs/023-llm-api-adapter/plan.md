# 023: Plan — LLM API Adapters (OpenAI-compatible + Anthropic)

**Branch**: `023-llm-api-adapter` | **Date**: 2026-03-26 | **Spec**: `specs/023-llm-api-adapter/spec.md`

## Summary

Dwa nowe adaptery LLM via HTTP: (1) OpenAI-compatible (`/v1/chat/completions`), (2) Anthropic Messages (`/v1/messages`). Wspólny parser response. Konfiguracja w application.yml, przełączanie przez `llm.adapter`.

## Technical Context

**Reużycie**: `LlmAnalyzer` port (bez zmian), `PromptBuilder`, `LlmAssessment`, `LlmCost`, WebClient (Spring WebFlux)
**Nowe**: `LlmResponseParser` (wspólna logika parsowania JSON), `OpenAiApiAdapter`, `AnthropicApiAdapter`
**Testing**: MockWebServer (HTTP mock), JUnit 5

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal | OK | Nowe adaptery w `adapter/out/llm/`, port bez zmian |
| II. Provider Abstraction | OK | `@ConditionalOnProperty` — przełączanie bez zmiany domain |
| III. BDD | OK | Feature file PRZED implementacją |
| III-b. Test Pyramid | OK | Unit + BDD + integration (MockWebServer) |
| VI. Static Analysis | OK | SpotBugs + PMD + ESLint before PR |

## Refaktor: Extract LlmResponseParser

Logika parsowania JSON response (lines 111-225 w `ClaudeCliAdapter`) jest wspólna dla wszystkich adapterów. Extract do reużywalnej klasy:

```java
public class LlmResponseParser {
    // Extracted from ClaudeCliAdapter:
    public LlmAssessment parseJsonResponse(String jsonText, String provider) { ... }
    // Parses: scoreAdjustment, comment, overallAutomatability, categories,
    //         humanOversightRequired, whyLlmFriendly, summaryTable

    public LlmCost parseCost(int inputTokens, int outputTokens, double costUsd, int durationMs) { ... }
}
```

## Warstwy zmian

### Warstwa 1: Extract LlmResponseParser (refaktor)

1. **`LlmResponseParser`** w `adapter/out/llm/`:
   - Extract `parseResponse()` z ClaudeCliAdapter → reużywalny parser
   - Input: raw JSON string (content z LLM response) + provider name
   - Output: `LlmAssessment`
   - Trzyma: `parseCategories`, `parseHumanOversight`, `parseStringArray`, `parseSummaryTable`

2. **Refaktor `ClaudeCliAdapter`** — deleguje do `LlmResponseParser` zamiast własnego parsowania

### Warstwa 2: OpenAI-compatible API Adapter

3. **`OpenAiApiAdapter`** w `adapter/out/llm/`:
   ```java
   @Component
   @ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "openai-api")
   public class OpenAiApiAdapter implements LlmAnalyzer {
       // WebClient → POST /v1/chat/completions
       // Request: {"model":"...", "messages":[{"role":"user","content":"<prompt>"}],
       //           "temperature":0.1, "max_tokens":2000}
       // Response: {"choices":[{"message":{"content":"<json>"}}], "usage":{...}}
   }
   ```

   **Request format**:
   ```json
   {
     "model": "gpt-4o",
     "messages": [{"role": "user", "content": "<prompt from PromptBuilder>"}],
     "temperature": 0.1,
     "max_tokens": 2000
   }
   ```

   **Response parsing**:
   - Extract `choices[0].message.content` → pass to `LlmResponseParser`
   - Extract `usage.prompt_tokens`, `usage.completion_tokens` → `LlmCost`

### Warstwa 3: Anthropic Messages API Adapter

4. **`AnthropicApiAdapter`** w `adapter/out/llm/`:
   ```java
   @Component
   @ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "anthropic-api")
   public class AnthropicApiAdapter implements LlmAnalyzer {
       // WebClient → POST /v1/messages
       // Headers: anthropic-version, x-api-key
       // Request: {"model":"...", "messages":[{"role":"user","content":"<prompt>"}],
       //           "max_tokens":2000}
       // Response: {"content":[{"text":"<json>"}], "usage":{...}}
   }
   ```

   **Headers specyficzne**:
   - `x-api-key: <key>` (nie Bearer token)
   - `anthropic-version: 2023-06-01`
   - `content-type: application/json`

   **Response parsing**:
   - Extract `content[0].text` → pass to `LlmResponseParser`
   - Extract `usage.input_tokens`, `usage.output_tokens` → `LlmCost`

### Warstwa 4: Konfiguracja

5. **`application.yml`** — nowe sekcje:
   ```yaml
   mr-analizer:
     llm:
       adapter: claude-cli  # claude-cli | openai-api | anthropic-api | none
       openai-api:
         url: https://api.openai.com
         key: ${LLM_API_KEY:}
         model: gpt-4o
         temperature: 0.1
         max-tokens: 2000
         timeout-seconds: 60
       anthropic-api:
         url: https://api.anthropic.com
         key: ${ANTHROPIC_API_KEY:}
         model: claude-sonnet-4-20250514
         temperature: 0.1
         max-tokens: 2000
         timeout-seconds: 60
   ```

### Warstwa 5: Testy

6. **`LlmResponseParserTest`** — unit test na wspólny parser
7. **`OpenAiApiAdapterTest`** — unit test z MockWebServer: happy path, timeout, 401, invalid JSON
8. **`AnthropicApiAdapterTest`** — unit test z MockWebServer: happy path, headers, timeout, 401
9. **BDD feature file** — scenariusze przełączania adapterów
10. **Refaktor istniejących testów** `ClaudeCliAdapterTest` — verify delegacja do parsera

## Kolejność implementacji

```
Phase 1 — BDD feature file
Phase 2 — LlmResponseParser (extract + unit test)
Phase 3 — Refaktor ClaudeCliAdapter → uses LlmResponseParser
Phase 4 — OpenAiApiAdapter + unit test + MockWebServer
Phase 5 — AnthropicApiAdapter + unit test + MockWebServer
Phase 6 — application.yml config
Phase 7 — BDD steps + all green + static analysis
```

## Ryzyka

| Ryzyko | Mitigation |
|--------|------------|
| LLM response format różni się między providerami | Wspólny PromptBuilder wymusza JSON format; parser toleruje warianty |
| API key w application.yml | Env variable: `${LLM_API_KEY:}`, nigdy hardcoded |
| Rate limiting OpenAI/Anthropic | Graceful error handling, retry NOT implemented (YAGNI) |
