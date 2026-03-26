# 023: LLM API Adapter — OpenAI-compatible HTTP + Anthropic Messages API

**Feature Branch**: `023-llm-api-adapter`
**Created**: 2026-03-26
**Status**: Draft
**Input**: Dodanie dwóch nowych adapterów LLM obok istniejącego Claude CLI: (1) OpenAI-compatible API (działa z OpenRouter, LiteLLM, vLLM, Ollama), (2) Anthropic Messages API (natywne). Łatwe przełączanie w application.yml.

## Problem

Obecny adapter LLM (`ClaudeCliAdapter`) wymaga zainstalowanego Claude CLI jako subprocess. To ogranicza:
- Brak wsparcia dla innych LLM (OpenAI, Gemini, Mistral, lokalne modele)
- Brak LLM proxy (OpenRouter, LiteLLM) — popularny pattern w enterprise
- Brak natywnej integracji z Anthropic API (Messages API)
- Koszt: CLI ma overhead startowy per request

Potrzebne są dwa nowe adaptery HTTP:
1. **OpenAI-compatible** (`POST /v1/chat/completions`) — standard de facto, obsługiwany przez OpenRouter, LiteLLM, vLLM, Ollama, Azure OpenAI
2. **Anthropic Messages** (`POST /v1/messages`) — natywny, lepsze structured output, tool use

## User Scenarios & Testing

### US1 — OpenAI-compatible API adapter (Priority: P1)

**Acceptance Scenarios**:

1. **Given** konfiguracja `llm.adapter=openai-api` z URL i API key, **When** system analizuje PR, **Then** wysyła POST `/v1/chat/completions` z promptem i parsuje response.
2. **Given** konfiguracja z modelem `gpt-4o`, **When** analiza, **Then** request zawiera `"model": "gpt-4o"`.
3. **Given** API zwraca timeout, **When** analiza, **Then** graceful fallback z `LlmAssessment(0.0, "timeout", ...)`.
4. **Given** API zwraca 401, **When** analiza, **Then** czytelny błąd auth.
5. **Given** response z JSON w `choices[0].message.content`, **When** parsowanie, **Then** wyciąga scoreAdjustment, comment, categories, oversight — identyczny format jak Claude CLI.

### US2 — Anthropic Messages API adapter (Priority: P1)

**Acceptance Scenarios**:

1. **Given** konfiguracja `llm.adapter=anthropic-api` z API key, **When** system analizuje PR, **Then** wysyła POST `/v1/messages` z `anthropic-version` header.
2. **Given** response z `content[0].text`, **When** parsowanie, **Then** wyciąga structured JSON identycznie jak z OpenAI.
3. **Given** API zwraca `usage` z input/output tokens, **When** parsowanie, **Then** `LlmCost` zawiera token counts i koszt.

### US3 — Przełączanie adapterów (Priority: P1)

**Acceptance Scenarios**:

1. **Given** `llm.adapter=openai-api`, **When** aplikacja startuje, **Then** aktywny jest OpenAI adapter.
2. **Given** `llm.adapter=anthropic-api`, **When** aplikacja startuje, **Then** aktywny jest Anthropic adapter.
3. **Given** `llm.adapter=claude-cli`, **When** aplikacja startuje, **Then** aktywny jest CLI adapter (backward compatible).
4. **Given** `llm.adapter=none`, **When** aplikacja startuje, **Then** aktywny jest NoOp adapter.
5. **Given** zmiana adapter w yml + restart, **When** analiza, **Then** nowy adapter jest używany.

### US4 — Konfiguracja modelu i parametrów (Priority: P2)

**Acceptance Scenarios**:

1. **Given** `model=claude-sonnet-4-20250514`, **When** request, **Then** model w body jest poprawny.
2. **Given** `temperature=0.1`, **When** request, **Then** temperature w body jest 0.1.
3. **Given** `max-tokens=2000`, **When** request, **Then** max_tokens w body jest 2000.

## Scope & Constraints

### In scope
- Adapter OpenAI-compatible API (WebClient HTTP)
- Adapter Anthropic Messages API (WebClient HTTP)
- Konfiguracja w application.yml (url, key, model, temperature, max-tokens)
- Parsowanie response → LlmAssessment (reużycie logiki z ClaudeCliAdapter)
- Token usage → LlmCost
- Error handling (timeout, auth, rate limit, invalid response)
- BDD scenariusze + unit testy + integration test z MockWebServer

### Out of scope
- Streaming response (future)
- Tool use / function calling (future)
- Batch API
- UI do wyboru adaptera (yml only)

## Technical Notes

- Reużycie `PromptBuilder` — prompt jest identyczny niezależnie od adaptera
- Reużycie logiki parsowania JSON response — extract do wspólnej klasy `LlmResponseParser`
- WebClient (Spring WebFlux) — już w projekcie, reużycie
- Konfiguracja: `@ConditionalOnProperty(name = "mr-analizer.llm.adapter", havingValue = "openai-api")`
- Anthropic API wymaga headera `anthropic-version: 2023-06-01` i `x-api-key` zamiast `Bearer` token
- OpenAI format: `{"model":"...", "messages":[{"role":"user","content":"..."}], "temperature":0.1}`
- Anthropic format: `{"model":"...", "messages":[{"role":"user","content":"..."}], "max_tokens":2000}`
