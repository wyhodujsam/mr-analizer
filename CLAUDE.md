# mr_analizer Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-21

## Active Technologies
- Java 17 + Spring Boot 3.x (Web, Data JPA, WebFlux client) — backend
- React 18 + TypeScript + Vite — frontend
- Cucumber 7 + JUnit 5 + Mockito (backend testing)
- Vitest 4.x + React Testing Library (frontend testing, requires Node 22+)
- H2 (dev database)
- Bootstrap 5 / React-Bootstrap (UI)

## Project Structure

```text
backend/                              # Spring Boot REST API (hexagonal: domain/, application/, adapter/)
frontend/                             # React + TypeScript SPA
specs/                                # SDD feature specs (001-007)
specs/bugs/                           # Known bugs documentation
.specify/                             # Spec Kit infrastructure
```

## Architecture

Hexagonal (ports & adapters) backend + React SPA frontend. See `.specify/memory/constitution.md` for rules.

LLM adapter: `claude-cli` (configurable in `application.yml`: `mr-analizer.llm.adapter`). Prompt in Polish via `PromptBuilder.DEFAULT_TEMPLATE`.

## Testing Approach — BDD + Unit + Frontend

- BDD: `.feature` files (Gherkin) BEFORE implementation, step defs in `bdd/steps/`
- Unit: JUnit 5 + Mockito for domain logic
- Integration: `IntegrationTest.java` — REST API round-trip including MR metadata persistence
- Frontend: Vitest + React Testing Library — null safety, navigation, component rendering
- Run backend: `cd backend && mvn test`
- Run frontend: `cd frontend && npx vitest run` (requires Node 22+, use nvm)

## Commands

```bash
# Backend
cd backend && mvn clean install       # build
cd backend && mvn test                # all tests (unit + Cucumber + integration)
cd backend && mvn spring-boot:run     # run on port 8083

# Frontend
cd frontend && npm install            # install deps
cd frontend && npm run dev            # dev server on port 3000
cd frontend && npm run build          # production build
cd frontend && npm test               # run Vitest tests
```

## Code Style

- Java 17: Follow standard conventions. Lombok for boilerplate reduction.
- TypeScript: Strict mode. Functional components with hooks.
- UI text: Polish language throughout frontend and LLM prompt.

## Domain Models (key)

- `LlmAssessment` — extended with: `overallAutomatability`, `categories`, `humanOversightRequired`, `whyLlmFriendly`, `summaryTable`. Backward-compatible 3-arg constructor.
- `AnalysisResult` — includes LLM detail fields + `hasDetailedAnalysis()` method.
- `AnalysisCategory`, `HumanOversightItem`, `SummaryAspect` — records for structured LLM response.

## Domain Exceptions

Domain-level exceptions live in `domain/exception/` — adapters wrap provider-specific errors into these:
- `ProviderException` — base for VCS provider failures
- `ProviderRateLimitException` — rate limit exceeded
- `ProviderAuthException` — auth failure
- `ReportNotFoundException` — report/result not found
- `InvalidRequestException` — input validation failure

`GlobalExceptionHandler` catches each type with dedicated `@ExceptionHandler` (no string matching).

## Important Notes

- GITHUB_TOKEN: read from file `~/mr_analizer/tmp.txt`, NEVER paste in code/conversation (GitHub revokes)
- ClaudeCliAdapter: stdin redirected from `/dev/null` to avoid warning that breaks JSON parsing
- MR metadata (branches, state, dates, labels, diffStats) fully persisted in AnalysisResultEntity
- Frontend null-safe: all nullable API fields handled with `?? []` / `?? 0` / `?? '\u2014'`

## Recent Changes
- 007-llm-analysis-details: Detailed LLM analysis (categories, oversight, summary), dedicated AnalysisDetailPage, frontend tests (Vitest), Polish translation, MR metadata persistence fix, Claude CLI stdin fix
- 003-cleanup: Split AnalyzeMrService (command/query), domain exceptions, validation, NPE fixes, GlobalExceptionHandler cleanup
- 002-mr-browse-analyze: Browse flow, repo CRUD, cache, MR selection
- 001-mvp-core: Initial setup, scoring engine, GitHub adapter, React dashboard
