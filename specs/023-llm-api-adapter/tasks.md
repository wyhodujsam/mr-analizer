# 023: Tasks — LLM API Adapters

**Input**: `specs/023-llm-api-adapter/spec.md`, `specs/023-llm-api-adapter/plan.md`

## Phase 1: BDD Feature File

- [ ] T01 `llm-adapters.feature` — scenariusze: OpenAI adapter happy path, Anthropic adapter happy path, przełączanie adapterów, error handling (timeout, auth)

## Phase 2: Extract LlmResponseParser

- [ ] T02 `LlmResponseParser` w `adapter/out/llm/LlmResponseParser.java`:
  - `parseJsonResponse(String rawText, String provider)` → `LlmAssessment`
  - Extract z ClaudeCliAdapter: find JSON in text, parse scoreAdjustment, comment, categories, humanOversight, whyLlmFriendly, summaryTable
  - `parseCostFromUsage(int inputTokens, int outputTokens, int cacheRead, int cacheCreation, double costUsd, int durationMs)` → `LlmCost`
  - Config fields: `responseScoreField`, `responseCommentField` (injectable)

- [ ] T03 `LlmResponseParserTest` — unit:
  - Happy path: JSON z wszystkimi polami → pełny LlmAssessment
  - Minimal JSON: tylko scoreAdjustment + comment → LlmAssessment z pustymi listami
  - JSON w markdown: ```json...``` → parser wyciąga JSON
  - Brak JSON → fallback LlmAssessment
  - Score clamping: >0.5 → 0.5, <-0.5 → -0.5

## Phase 3: Refaktor ClaudeCliAdapter

- [ ] T04 Refaktor `ClaudeCliAdapter`:
  - Inject `LlmResponseParser`
  - Deleguj `parseResponse()` → `parser.parseJsonResponse()`
  - Zachowaj `parseCost()` na level root JSON (CLI-specific: `usage` i `total_cost_usd` w root)
  - Verify: istniejące testy `ClaudeCliAdapterTest` nadal green

## Phase 4: OpenAI-compatible API Adapter

- [ ] T05 `OpenAiApiAdapter` w `adapter/out/llm/OpenAiApiAdapter.java`:
  - `@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "openai-api")`
  - WebClient: POST `{url}/v1/chat/completions`
  - Headers: `Authorization: Bearer {key}`, `Content-Type: application/json`
  - Body: `{"model":"...", "messages":[{"role":"user","content":"<prompt>"}], "temperature":..., "max_tokens":...}`
  - Response: `choices[0].message.content` → `LlmResponseParser.parseJsonResponse()`
  - Usage: `usage.prompt_tokens`, `usage.completion_tokens` → `LlmCost`
  - Error handling: timeout → fallback, 401 → auth error, 429 → rate limit, other → generic error
  - Timeout: configurable via WebClient `.timeout(Duration.ofSeconds(...))`

- [ ] T06 `OpenAiApiAdapterTest` — unit z MockWebServer:
  - Happy path: mock 200 z valid JSON → LlmAssessment z score, comment, categories
  - Token usage: verify LlmCost populated
  - Timeout: mock delay > timeout → graceful fallback
  - Auth error: mock 401 → LlmAssessment z error message
  - Rate limit: mock 429 → LlmAssessment z error message
  - Invalid JSON in response: → fallback
  - Verify: correct request body (model, messages, temperature)
  - Verify: correct headers (Authorization Bearer)

## Phase 5: Anthropic Messages API Adapter

- [ ] T07 `AnthropicApiAdapter` w `adapter/out/llm/AnthropicApiAdapter.java`:
  - `@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "anthropic-api")`
  - WebClient: POST `{url}/v1/messages`
  - Headers: `x-api-key: {key}`, `anthropic-version: 2023-06-01`, `Content-Type: application/json`
  - Body: `{"model":"...", "messages":[{"role":"user","content":"<prompt>"}], "max_tokens":...}`
  - Response: `content[0].text` → `LlmResponseParser.parseJsonResponse()`
  - Usage: `usage.input_tokens`, `usage.output_tokens` → `LlmCost`
  - Error handling: analogicznie do OpenAI

- [ ] T08 `AnthropicApiAdapterTest` — unit z MockWebServer:
  - Happy path: mock 200 z Anthropic response format → LlmAssessment
  - Verify: headers `x-api-key` (nie Bearer), `anthropic-version`
  - Verify: request body (model, messages, max_tokens)
  - Token usage → LlmCost
  - Timeout, 401, 429, invalid response → graceful fallback

## Phase 6: Configuration

- [ ] T09 Rozszerzenie `application.yml`:
  ```yaml
  mr-analizer:
    llm:
      adapter: claude-cli
      openai-api:
        url: ${LLM_API_URL:https://api.openai.com}
        key: ${LLM_API_KEY:}
        model: gpt-4o
        temperature: 0.1
        max-tokens: 2000
        timeout-seconds: 60
      anthropic-api:
        url: ${ANTHROPIC_API_URL:https://api.anthropic.com}
        key: ${ANTHROPIC_API_KEY:}
        model: claude-sonnet-4-20250514
        temperature: 0.1
        max-tokens: 2000
        timeout-seconds: 60
  ```

## Phase 7: BDD Steps + All Green + Static Analysis

- [ ] T10 BDD step definitions `LlmAdapterSteps.java`
- [ ] T11 Run ALL: `mvn test` → green, `mvn verify -DskipTests` → SpotBugs+PMD clean
- [ ] T12 Run frontend: `npx vitest run` + `npm run lint` → green
- [ ] T13 Update CLAUDE.md — LLM adapters section

## Dependency Graph

```
T01 (BDD)
  ↓
T02-T03 (Parser extract + test)
  ↓
T04 (Refaktor ClaudeCliAdapter)
  ↓
T05-T06 (OpenAI adapter + test) ─── T07-T08 (Anthropic adapter + test)
  ↓
T09 (config)
  ↓
T10-T13 (BDD steps + all green)
```

## Checklist z constitution (III-b + VI)

- [ ] Unit testy: LlmResponseParser, OpenAiApiAdapter, AnthropicApiAdapter
- [ ] BDD .feature file (test-first)
- [ ] Integration test z MockWebServer (OpenAI + Anthropic)
- [ ] Concurrency: nie dotyczy (stateless adaptery)
- [ ] Static analysis: `mvn verify -DskipTests` + `npm run lint`
