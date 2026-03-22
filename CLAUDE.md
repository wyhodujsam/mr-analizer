# mr_analizer Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-21

## Active Technologies
- Java 17 + Spring Boot 3.x (Web, Data JPA, WebFlux client) — backend
- React 18 + TypeScript + Vite — frontend
- Cucumber 7 + JUnit 5 + Mockito (backend testing)
- Vitest 4.x + React Testing Library (frontend testing, requires Node 22+)
- H2 (dev database)
- Bootstrap 5 / React-Bootstrap (UI)
- Spring Boot Actuator + Micrometer (performance metrics, dev profile)
- async-profiler v3.0 (CPU/alloc flame graphs, external CLI tool)

## Project Structure

```text
backend/                              # Spring Boot REST API (hexagonal: domain/, application/, adapter/)
frontend/                             # React + TypeScript SPA
specs/                                # SDD feature specs (001-012)
specs/bugs/                           # Known bugs documentation
scripts/                              # Dev tools (profile.sh)
reports/                              # Performance profiling reports (gitignored)
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
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev  # run with dev profile (H2 console + diagnostics)

# Frontend
cd frontend && npm install            # install deps
cd frontend && npm run dev            # dev server on port 3000
cd frontend && npm run build          # production build
cd frontend && npm test               # run Vitest tests

# Performance Profiling
/profile                              # Claude command: full profiling
/profile --with-load --duration 60    # profiling with load generation
bash scripts/profile.sh --help        # manual script usage
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

## Performance Profiling

- `/profile` Claude command runs `scripts/profile.sh` and analyzes results
- `DiagnosticsController` (`@Profile("dev")`) exposes `GET/POST /api/diagnostics/sql-stats` for Hibernate Statistics
- Actuator endpoints: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` (configured in application.yml)
- async-profiler: external CLI tool, install in `~/tools/async-profiler/` (see `specs/012-performance-profiling/quickstart.md`)
- Reports saved in `reports/` (gitignored), flame graphs in `reports/flamegraphs/`

## Activity Module (odseparowany)

Moduł analizy aktywności kontrybutora — wykrywanie nieprawidłowości (za duże PR-y, rubber-stamping, praca weekendowa, brak review).

- **Domain**: `domain/model/activity/`, `domain/service/activity/`, `domain/port/in/activity/`, `domain/port/out/activity/`
- **Rules** (Strategy pattern): `LargePrRule`, `QuickReviewRule`, `WeekendWorkRule`, `NightWorkRule`, `NoReviewRule`, `SelfMergeRule`, `AggregateRules`
- **REST API**: `ActivityController` — `/api/activity/{owner}/{repo}/contributors`, `/api/activity/{owner}/{repo}/report?author=`
- **Frontend**: `/activity` route, `ActivityDashboardPage`, heatmapa SVG (GitHub-style), drill-down
- **GitHub adapter**: `GitHubReviewAdapter` — fetches PR reviews via `/repos/{owner}/{repo}/pulls/{number}/reviews`

## Lessons Learned (code review 015)

### GitHub API: lista vs single PR
GitHub `GET /pulls` (lista) **nie zwraca** `additions`, `deletions`, `changed_files`. Te pola są tylko na `GET /pulls/{number}` (single). Każdy feature korzystający z rozmiaru PR-a musi fetchować single PR per item. Koszt: N dodatkowych API calls — używać parallel fetch (`CompletableFuture`) i ograniczać limit.

### N+1 problem z GitHub API
Moduł activity fetchuje N detail + N reviews = 2N calls. Mitigation:
- Parallel fetch via `CompletableFuture` (nie sekwencyjnie)
- Reviews tylko dla merged PRs (open/closed nie potrzebują)
- Limit 200 PRs max
- Rate limit: 5000/h authenticated — kilka raportów wyczerpie jeśli repo ma dużo PRs

### Domain purity w hexagonal
- **NIE** używać `@Service`/`@Component` w `domain/` — rejestracja beanów w `adapter/config/`
- Porty (`ReviewProvider`) powinny być provider-agnostic — `String prId` zamiast `int prNumber`
- Reguły domenowe (ActivityRule) instantiated w `@Configuration`, nie `@Component`

### CORS
- **NIE** używać `@CrossOrigin(origins = "*")` na controllerach — to nadpisuje globalny `CorsConfig`
- Globalna konfiguracja CORS jest w `adapter/config/CorsConfig.java`

### Walidacja input w REST
- Path variables (`owner`, `repo`) wymagają walidacji regexem — mogą zawierać niebezpieczne znaki
- Request params (`author`) wymagają limitu długości
- Używać `InvalidRequestException` → łapany przez `GlobalExceptionHandler` → 400

### Testy: mock musi pokrywać cały flow
- Jeśli service woła `fetchMergeRequests` (lista) + `fetchMergeRequest` (detail), testy muszą mockować **oba**
- Unit testy z mockami nie wyłapią problemów integracyjnych (np. GitHub API nie zwraca pól na liście) — rozważyć contract testy

## Recent Changes
- 015-user-activity-health: Activity dashboard, 6 detection rules, heatmap, BDD + unit tests
- 012-performance-profiling: `/profile` command, DiagnosticsController, Actuator+Micrometer, async-profiler integration, `scripts/profile.sh`
- 007-llm-analysis-details: Detailed LLM analysis (categories, oversight, summary), dedicated AnalysisDetailPage, frontend tests (Vitest), Polish translation, MR metadata persistence fix, Claude CLI stdin fix
- 003-cleanup: Split AnalyzeMrService (command/query), domain exceptions, validation, NPE fixes, GlobalExceptionHandler cleanup
- 002-mr-browse-analyze: Browse flow, repo CRUD, cache, MR selection
- 001-mvp-core: Initial setup, scoring engine, GitHub adapter, React dashboard
