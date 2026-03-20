# Tasks: MVP Core

**Input**: Design documents from `/specs/001-mvp-core/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/rest-api.md

## Model Selection Analysis

| Task | Description | Recommended Model | Rationale |
|------|-------------|-------------------|-----------|
| T001 | Maven project setup | Haiku | Boilerplate, standard Spring Initializr pattern |
| T002 | React+Vite project setup | Haiku | Boilerplate, standard Vite scaffold |
| T003 | .gitignore updates | Haiku | Trivial config file |
| T004 | Domain model classes (9 entities) | Sonnet | Many fields, relationships to get right |
| T005 | Rule interface + RuleResult | Sonnet | Interface design, domain contract |
| T006 | ScoringEngine + ScoringConfig | Opus | Core business logic, rule orchestration, edge cases |
| T007 | Port interfaces (in + out) | Sonnet | Interface design, hexagonal contracts |
| T008 | InMemoryAnalysisResultRepository stub | Haiku | Simple in-memory map implementation |
| T009 | AnalyzeMrService | Opus | Orchestration use case, coordinates providers+rules+LLM+persistence |
| T010 | Application DTOs | Haiku | Simple data classes |
| T011 | scoring.feature (BDD) | Sonnet | Map spec acceptance criteria to Gherkin |
| T012 | Cucumber runner + Spring config | Sonnet | Framework integration setup |
| T013 | ScoringSteps (step definitions) | Opus | Spring integration, domain assertions |
| T014 | ExcludeRuleTest | Sonnet | Multiple exclusion scenarios |
| T015 | BoostRuleTest | Sonnet | Multiple boost scenarios |
| T016 | PenalizeRuleTest | Sonnet | Multiple penalty scenarios |
| T017 | ScoringEngineTest | Opus | Full pipeline test, edge cases, threshold boundaries |
| T018 | ExcludeRule, BoostRule, PenalizeRule impl | Opus | Business logic, config-driven rules |
| T019 | RulesConfig | Sonnet | Spring config, YAML binding |
| T020 | application.yml | Sonnet | Scoring + rules + provider + LLM config |
| T021 | analysis.feature (BDD) | Sonnet | Map US1 acceptance criteria to Gherkin |
| T022 | AnalysisSteps (step definitions) | Opus | Mock provider, full analysis flow assertions |
| T023 | provider.feature (BDD) | Sonnet | GitHub fetch, errors, rate limit, pagination |
| T024 | ProviderSteps (step definitions) | Opus | Mock WebClient, API simulation |
| T025 | GitHub DTO classes | Haiku | Simple POJOs mirroring GitHub API |
| T026 | GitHubClient (WebClient) | Opus | Async HTTP, pagination (Link header), rate limit, auth |
| T027 | GitHubMapper | Sonnet | DTO→domain mapping, hasTests detection |
| T028 | GitHubAdapter | Sonnet | Implements port, delegates to client+mapper |
| T029 | ProviderConfig | Sonnet | Spring conditional bean config |
| T030 | CorsConfig | Haiku | Simple CORS setup for dev |
| T031 | REST controller DTOs | Sonnet | AnalysisResponse, MrDetailResponse, ErrorResponse |
| T032 | AnalysisRestController (5 endpoints) | Opus | Maps REST to use cases, handles all endpoints from contract |
| T033 | GlobalExceptionHandler | Sonnet | Maps domain exceptions to HTTP errors |
| T034 | ClaudeCliAdapter | Opus | ProcessBuilder, timeout, JSON parsing, error handling, fallback |
| T035 | NoOpLlmAdapter | Haiku | Trivial neutral implementation |
| T036 | LlmConfig | Sonnet | Spring conditional bean config |
| T037 | LLM scenarios in analysis.feature | Sonnet | Claude-cli, none, timeout scenarios |
| T038 | JPA entities | Sonnet | AnalysisResultEntity, AnalysisReportEntity, JSON fields |
| T039 | SpringData repository interface | Haiku | Extends JpaRepository |
| T040 | JpaAnalysisResultRepository | Sonnet | Implements domain port, entity↔domain mapping |
| T041 | H2 config in application.yml | Haiku | Datasource config |
| T042 | TypeScript types | Haiku | Mirror backend DTOs |
| T043 | Axios API client | Sonnet | Typed API functions, error handling |
| T044 | Layout + navbar | Haiku | Bootstrap boilerplate |
| T045 | AnalysisForm component | Sonnet | Form state, validation, submit, loading |
| T046 | ScoreBadge component | Haiku | Colored badge by verdict |
| T047 | MrTable component | Sonnet | Sortable, colored rows, click navigation |
| T048 | SummaryCard component | Haiku | Three cards with counts/percentages |
| T049 | DashboardPage | Opus | Orchestrates form+table+summary, API calls, empty state handling, error display |
| T050 | MrDetailPage | Sonnet | Score breakdown table, LLM comment, API call |
| T051 | App.tsx + routing | Haiku | React Router setup |
| T052 | app.css | Haiku | Verdict colors, responsive layout |
| T053 | Vite proxy config | Haiku | /api → localhost:8083 |
| T054 | AnalyzeMrServiceTest | Opus | Mocking multiple dependencies, orchestration verification |
| T055 | Integration test (Spring + H2) | Opus | Full context, mocked GitHub (MockWebServer), end-to-end |
| T056 | application-test.yml | Haiku | Test config |
| T057 | Run all tests + quickstart validation | Sonnet | Verify everything works together |

## Phase 1: Setup

**Purpose**: Project initialization and basic structure

- [ ] T001 [P] Create backend Maven project with Spring Boot 3.x, Java 17, dependencies (Web, Data JPA, Validation, WebFlux, Lombok, H2, Cucumber, JUnit 5, Mockito) in `backend/pom.xml`
- [ ] T002 [P] Create frontend React+TypeScript project with Vite, React-Bootstrap, Axios, React Router in `frontend/` (npm create vite, install deps)
- [ ] T003 [P] Update `.gitignore` for backend (target/) and frontend (node_modules/, dist/)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain model, ports, scoring engine, and core infrastructure that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 [P] Create domain model classes in `backend/src/main/java/com/mranalizer/domain/model/`: MergeRequest, ChangedFile, DiffStats, AnalysisResult, AnalysisReport, Verdict, FetchCriteria, LlmAssessment, RuleResult
- [ ] T005 [P] Create Rule interface in `backend/src/main/java/com/mranalizer/domain/rules/Rule.java` and RuleResult in `domain/rules/RuleResult.java` — interface only, implementations in Phase 3
- [ ] T006 Create ScoringEngine and ScoringConfig in `backend/src/main/java/com/mranalizer/domain/scoring/` — evaluates list of Rules against MergeRequest, calculates score, assigns verdict based on thresholds
- [ ] T007 [P] Create port interfaces in `backend/src/main/java/com/mranalizer/domain/port/`: in/ (AnalyzeMrUseCase, GetAnalysisResultsUseCase), out/ (MergeRequestProvider, LlmAnalyzer, AnalysisResultRepository)
- [ ] T008 [P] Create InMemoryAnalysisResultRepository in `backend/src/main/java/com/mranalizer/adapter/out/persistence/InMemoryAnalysisResultRepository.java` — temporary stub implementing AnalysisResultRepository port with ConcurrentHashMap, allows AnalyzeMrService to compile before JPA is ready
- [ ] T009 Create AnalyzeMrService in `backend/src/main/java/com/mranalizer/application/AnalyzeMrService.java` — implements AnalyzeMrUseCase + GetAnalysisResultsUseCase, orchestrates: fetch MRs via provider → evaluate rules via ScoringEngine → optionally call LlmAnalyzer → save via repository → return AnalysisReport
- [ ] T010 [P] Create application DTOs in `backend/src/main/java/com/mranalizer/application/dto/`: AnalysisRequestDto, AnalysisSummaryDto

**Checkpoint**: Domain model, scoring engine, ports, and use case service compile and are ready for testing

---

## Phase 3: User Story 2 - Silnik regul i scoring (Priority: P1) MVP

**Goal**: Working rule engine that evaluates PRs and assigns scores/verdicts

**Independent Test**: Given PR data, scoring engine returns correct score and verdict per configured rules

### BDD Tests for User Story 2 (test-first)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US2] Create `backend/src/test/resources/features/scoring.feature` — scenarios: exclude by hotfix label, exclude by min files (<2), exclude by file-extensions-only (.yml/.toml), boost by refactor keyword, boost by has-tests, penalize by large diff (>500), penalize by no description, full scoring pipeline (base + boosts + penalties = verdict)
- [ ] T012 [P] [US2] Create Cucumber runner `backend/src/test/java/com/mranalizer/bdd/CucumberTestRunner.java` (@Suite @SelectClasspathResource) and Spring config `CucumberSpringConfig.java` (@CucumberContextConfiguration @SpringBootTest)
- [ ] T013 [US2] Create step definitions `backend/src/test/java/com/mranalizer/bdd/steps/ScoringSteps.java` — Given PR with specific properties, When evaluate rules, Then assert score and verdict

### Unit Tests for User Story 2

- [ ] T014 [P] [US2] Create `backend/src/test/java/com/mranalizer/domain/rules/ExcludeRuleTest.java` — test: label exclusion (hotfix/security/emergency), min-changed-files (<2), max-changed-files (>50), file-extensions-only (.env/.yml/.toml/.lock), no-description exclusion
- [ ] T015 [P] [US2] Create `backend/src/test/java/com/mranalizer/domain/rules/BoostRuleTest.java` — test: description keywords (refactor/cleanup/add test/rename), has-tests boost, changed-files-range (3-15 sweet spot), labels boost (tech-debt/refactoring/chore)
- [ ] T016 [P] [US2] Create `backend/src/test/java/com/mranalizer/domain/rules/PenalizeRuleTest.java` — test: large-diff (>500 lines), no-description penalty, touches-config penalty
- [ ] T017 [US2] Create `backend/src/test/java/com/mranalizer/domain/scoring/ScoringEngineTest.java` — test: base score 0.5, boosts add to score, penalties subtract, exclude overrides everything, threshold boundaries (0.39→NOT_SUITABLE, 0.4→MAYBE, 0.69→MAYBE, 0.7→AUTOMATABLE), empty rules → base score, all excluded → score 0

### Implementation for User Story 2

- [ ] T018 [US2] Create rule implementations in `backend/src/main/java/com/mranalizer/domain/rules/`: ExcludeRule (label, min/max files, extensions-only), BoostRule (keywords, has-tests, files-range, labels), PenalizeRule (large-diff, no-description, touches-config) — each implements Rule interface, configurable via constructor params
- [ ] T019 [US2] Create RulesConfig in `backend/src/main/java/com/mranalizer/adapter/config/RulesConfig.java` — reads rules config from application.yml, creates Rule beans (list of ExcludeRule, BoostRule, PenalizeRule)
- [ ] T020 [US2] Create `backend/src/main/resources/application.yml` with: server.port=8083, scoring config (base-score: 0.5, thresholds), rules config (exclude/boost/penalize from SPEC.md), provider placeholder, LLM placeholder, H2 datasource placeholder

**Checkpoint**: Scoring engine works, BDD scenarios PASS, unit tests PASS

---

## Phase 4: User Story 1 - Analiza repozytorium GitHub (Priority: P1)

**Goal**: Fetch PRs from GitHub API, analyze them, expose results via REST API

**Independent Test**: User sends POST /api/analysis → gets report with scored PRs

### BDD Tests for User Story 1 (test-first)

- [ ] T021 [P] [US1] Create `backend/src/test/resources/features/analysis.feature` — scenarios: run analysis on repo returns report with results, each result has score and verdict, invalid repo returns error, missing token returns auth error
- [ ] T022 [US1] Create step definitions `backend/src/test/java/com/mranalizer/bdd/steps/AnalysisSteps.java` — Given repo with mocked provider, When POST /api/analysis, Then response contains report with scored results

### BDD Tests for provider

- [ ] T023 [P] [US1] Create `backend/src/test/resources/features/provider.feature` — scenarios: fetch PRs from GitHub returns MergeRequest list, rate limit (403) returns clear error, invalid token (401) returns auth error, repo with >100 PRs paginates automatically, PR with 0 changed files is skipped
- [ ] T024 [US1] Create step definitions `backend/src/test/java/com/mranalizer/bdd/steps/ProviderSteps.java` — mocked WebClient/MockWebServer

### Implementation for User Story 1

- [ ] T025 [P] [US1] Create GitHub DTO classes in `backend/src/main/java/com/mranalizer/adapter/out/provider/github/dto/`: GitHubPullRequest, GitHubFile — fields matching GitHub REST API v3
- [ ] T026 [US1] Create GitHubClient in `backend/src/main/java/com/mranalizer/adapter/out/provider/github/GitHubClient.java` — WebClient wrapper: auth header (Bearer token), pagination (parse Link header rel=next), rate limit check (X-RateLimit-Remaining), fetch PR list + files per PR
- [ ] T027 [US1] Create GitHubMapper in `backend/src/main/java/com/mranalizer/adapter/out/provider/github/GitHubMapper.java` — maps GitHubPullRequest+GitHubFile[] to domain MergeRequest, detects hasTests (files matching *Test*, *Spec*, *test/*, *spec/*), computes DiffStats
- [ ] T028 [US1] Create GitHubAdapter in `backend/src/main/java/com/mranalizer/adapter/out/provider/github/GitHubAdapter.java` — implements MergeRequestProvider, uses GitHubClient+Mapper, handles FetchCriteria (date filter, state, limit)
- [ ] T029 [US1] Create ProviderConfig in `backend/src/main/java/com/mranalizer/adapter/config/ProviderConfig.java` — @Configuration, creates MergeRequestProvider bean based on `mr-analizer.provider` property (github/gitlab)
- [ ] T030 [US1] Create CorsConfig in `backend/src/main/java/com/mranalizer/adapter/config/CorsConfig.java` — allow localhost:3000 origin for dev
- [ ] T031 [US1] Create REST controller DTOs in `backend/src/main/java/com/mranalizer/adapter/in/rest/dto/`: AnalysisResponse (reportId, projectSlug, counts, results list), MrDetailResponse (full MR data + scoreBreakdown list), ErrorResponse (error code + message)
- [ ] T032 [US1] Create AnalysisRestController in `backend/src/main/java/com/mranalizer/adapter/in/rest/AnalysisRestController.java` — endpoints per contract: POST /api/analysis, GET /api/analysis, GET /api/analysis/{reportId}, GET /api/analysis/{reportId}/mrs/{resultId}, GET /api/summary/{reportId}
- [ ] T033 [US1] Create GlobalExceptionHandler in `backend/src/main/java/com/mranalizer/adapter/in/rest/GlobalExceptionHandler.java` — @ControllerAdvice, maps: IllegalArgumentException→400, AuthenticationException→401, RateLimitException→429, NotFoundException→404, generic→500

**Checkpoint**: GitHub adapter fetches PRs, scoring engine evaluates them, REST API exposes results, CORS works

---

## Phase 5: User Story 3 - Wzbogacenie analizy przez LLM (Priority: P2)

**Goal**: Optional Claude CLI integration for deeper PR analysis

**Independent Test**: With claude-cli adapter enabled, analysis results include LLM comment and adjusted score

- [ ] T034 [US3] Create ClaudeCliAdapter in `backend/src/main/java/com/mranalizer/adapter/out/llm/ClaudeCliAdapter.java` — implements LlmAnalyzer: builds prompt with MR title+description+file list+diff stats, runs `claude -p "prompt" --output-format json` via ProcessBuilder, timeout 60s, parses JSON response for scoreAdjustment+comment, on timeout/error returns neutral LlmAssessment with "LLM timeout"/"LLM error" comment, checks `claude` availability in PATH at construction
- [ ] T035 [P] [US3] Create NoOpLlmAdapter in `backend/src/main/java/com/mranalizer/adapter/out/llm/NoOpLlmAdapter.java` — returns LlmAssessment(scoreAdjustment=0, comment=null, provider="none")
- [ ] T036 [US3] Create LlmConfig in `backend/src/main/java/com/mranalizer/adapter/config/LlmConfig.java` — @Configuration, creates LlmAnalyzer bean based on `mr-analizer.llm.adapter` property (claude-cli/none), logs warning if claude-cli selected but not in PATH
- [ ] T037 [US3] Add LLM-related scenarios to `backend/src/test/resources/features/analysis.feature` — scenarios: adapter=claude-cli returns result with LLM comment, adapter=none returns result without LLM comment, CLI timeout returns result with "LLM timeout" annotation

**Checkpoint**: LLM analysis works with Claude CLI, graceful fallback on error/timeout

---

## Phase 6: User Story 5 - Persystencja wynikow (Priority: P3)

**Goal**: Save analysis results to H2 database, replace in-memory stub

**Independent Test**: Run analysis, restart app, results still available

- [ ] T038 [P] [US5] Create JPA entities in `backend/src/main/java/com/mranalizer/adapter/out/persistence/entity/`: AnalysisResultEntity (@Entity, fields: id, externalMrId, mrTitle, mrAuthor, projectSlug, provider, score, verdict, reasons as JSON string, matchedRules as JSON string, llmComment, analyzedAt, reportId), AnalysisReportEntity (@Entity, @OneToMany→results, fields: id, projectSlug, provider, analyzedAt, totalMrs, counts)
- [ ] T039 [P] [US5] Create SpringDataAnalysisResultRepository interface (extends JpaRepository<AnalysisReportEntity, Long>) in `backend/src/main/java/com/mranalizer/adapter/out/persistence/SpringDataAnalysisResultRepository.java`
- [ ] T040 [US5] Create JpaAnalysisResultRepository in `backend/src/main/java/com/mranalizer/adapter/out/persistence/JpaAnalysisResultRepository.java` — implements domain AnalysisResultRepository port, maps entities↔domain, replaces InMemoryAnalysisResultRepository
- [ ] T041 [US5] Update application.yml with H2 datasource config: spring.datasource (url: jdbc:h2:file:./data/mranalizer for persistence, or jdbc:h2:mem:test for tests), spring.jpa (ddl-auto: update, show-sql: false), h2-console enabled

**Checkpoint**: Results persisted in H2, survive app restart

---

## Phase 7: User Story 4 - Dashboard React (Priority: P2)

**Goal**: React frontend with analysis form, results table, summary cards, MR detail page

**Independent Test**: Open browser, submit analysis, see colored results table with summary

- [ ] T042 [P] [US4] Create TypeScript types in `frontend/src/types/index.ts` — interfaces: AnalysisResponse, AnalysisResultItem, MrDetailResponse, ScoreBreakdownEntry, AnalysisSummary, VerdictCount, ErrorResponse; type Verdict = 'AUTOMATABLE' | 'MAYBE' | 'NOT_SUITABLE'
- [ ] T043 [P] [US4] Create API client in `frontend/src/api/analysisApi.ts` — Axios instance with baseURL '/api', typed functions: runAnalysis(request), getAnalyses(), getAnalysis(reportId), getMrDetail(reportId, resultId), getSummary(reportId), error handling
- [ ] T044 [P] [US4] Create Layout component in `frontend/src/components/Layout.tsx` — Bootstrap navbar ("MR Analizer"), Container, Outlet for nested routes
- [ ] T045 [US4] Create AnalysisForm component in `frontend/src/components/AnalysisForm.tsx` — controlled form: projectSlug (required), provider select (github), targetBranch, after/before date pickers, limit (default 100), useLlm checkbox, submit button with loading spinner, validation
- [ ] T046 [P] [US4] Create ScoreBadge component in `frontend/src/components/ScoreBadge.tsx` — Bootstrap Badge: green bg for AUTOMATABLE, yellow for MAYBE, red for NOT_SUITABLE, shows score number
- [ ] T047 [US4] Create MrTable component in `frontend/src/components/MrTable.tsx` — Bootstrap Table: columns (#, title, author, score, verdict), row background color by verdict, click row → navigate to /mr/:reportId/:resultId, sortable by score column
- [ ] T048 [P] [US4] Create SummaryCard component in `frontend/src/components/SummaryCard.tsx` — three Bootstrap Cards in a row: Automatable (green, count + %), Maybe (yellow), Not Suitable (red)
- [ ] T049 [US4] Create DashboardPage in `frontend/src/pages/DashboardPage.tsx` — combines AnalysisForm + SummaryCard + MrTable, manages state (loading, results, error), calls API on form submit, shows empty state message when no results or all excluded, shows error alert on API failure
- [ ] T050 [US4] Create MrDetailPage in `frontend/src/pages/MrDetailPage.tsx` — fetches MR detail from API, displays: MR metadata (title, author, branch, dates, labels), DiffStats, score + verdict badge, score breakdown table (rule name, type, weight, reason), LLM comment (if present), link to original PR
- [ ] T051 [US4] Create App.tsx with React Router v6: BrowserRouter, Routes: '/' → Layout+DashboardPage, '/mr/:reportId/:resultId' → Layout+MrDetailPage
- [ ] T052 [P] [US4] Create `frontend/src/styles/app.css` — verdict row colors (.verdict-automatable green bg, .verdict-maybe yellow, .verdict-not-suitable red), table hover, card styling, responsive breakpoints
- [ ] T053 [US4] Configure Vite proxy in `frontend/vite.config.ts` — server.proxy: '/api' → 'http://localhost:8083' with changeOrigin

**Checkpoint**: Full working dashboard — form, colored table, summary cards, MR detail with score breakdown, empty state handling

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration tests, final verification

- [ ] T054 Create AnalyzeMrServiceTest in `backend/src/test/java/com/mranalizer/application/AnalyzeMrServiceTest.java` — mock MergeRequestProvider, LlmAnalyzer, AnalysisResultRepository; verify: service calls provider with criteria, passes MRs through scoring engine, optionally calls LLM, saves report, returns correct AnalysisReport
- [ ] T055 Create integration test `backend/src/test/java/com/mranalizer/IntegrationTest.java` — @SpringBootTest with H2, MockWebServer for GitHub API, NoOp LLM; test: POST /api/analysis → verify 200 + results in DB, GET /api/analysis/{id} → verify report, GET /api/analysis/{id}/mrs/{resultId} → verify detail with breakdown
- [ ] T056 Create `backend/src/test/resources/application-test.yml` — test config: H2 in-memory, NoOp LLM, mock provider URL
- [ ] T057 Run all tests (`mvn test`), verify: all BDD scenarios pass (min 10), all unit tests pass (min 15), integration test passes. Run quickstart.md validation.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US2 Scoring (Phase 3)**: Depends on Phase 2 — scoring engine needed by all other stories
- **US1 GitHub+API (Phase 4)**: Depends on Phase 2+3 — needs scoring + domain model
- **US3 LLM (Phase 5)**: Depends on Phase 2 — can run parallel with Phase 4
- **US5 Persistence (Phase 6)**: Depends on Phase 2 — can run parallel with Phase 4/5
- **US4 Dashboard (Phase 7)**: Depends on Phase 4 (REST API must exist)
- **Polish (Phase 8)**: Depends on all phases

### Critical Fix: Compilation Dependencies

- AnalyzeMrService (T009) depends on AnalysisResultRepository port → InMemoryAnalysisResultRepository stub (T008) ensures compilation
- InMemoryAnalysisResultRepository is replaced by JpaAnalysisResultRepository in Phase 6 (T040)

### Parallel Opportunities

- T001, T002, T003 — all setup tasks in parallel
- T004, T005, T007, T008, T010 — domain model, rule interface, ports, stub repo, DTOs in parallel
- T011, T012, T014, T015, T016 — BDD features + unit tests in parallel
- T021, T023, T025 — US1 BDD + GitHub DTOs in parallel
- T034, T035 — LLM adapters in parallel
- T038, T039 — JPA entities + Spring Data repo in parallel
- T042, T043, T044, T046, T048, T052 — frontend types, API client, layout, badge, summary, css in parallel

---

## Implementation Strategy

### MVP First (US2 → US1 → US4)

1. Phase 1: Setup (backend + frontend scaffolding)
2. Phase 2: Foundational (domain model + ports + scoring engine + in-memory stub)
3. Phase 3: US2 — Scoring rules (BDD first → unit tests → implement rules)
4. Phase 4: US1 — GitHub adapter + REST API + CORS
5. Phase 7: US4 — React dashboard (needs REST API)
6. Phase 5: US3 — LLM integration (can be deferred)
7. Phase 6: US5 — Persistence / replace in-memory stub (can be deferred)
8. Phase 8: Polish + integration tests + validation
