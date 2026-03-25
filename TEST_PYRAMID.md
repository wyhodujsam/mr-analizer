# Test Pyramid — MR Analizer

Stan na: 2026-03-25

## Piramida

```
                      ▲
                     /E2E\              18 testów
                    / (3%) \            Playwright (Chromium headless)
                   /────────\
                  / Integr.  \          36 testów
                 /   (6%)     \         Spring Boot + MockWebServer + H2
                /──────────────\
               /      BDD       \       70 scenariuszy
              /     (12%)        \      Cucumber 7 / Gherkin
             /────────────────────\
            /     Unit Tests       \    ~289 backend + 159 frontend = 448
           /        (79%)           \   JUnit 5 + Mockito | Vitest + RTL
          /────────────────────────────\
```

**Total: ~572 testów**

## Szczegóły per warstwa

### Unit Tests (448) — najszybsze, najliczniejsze

**Backend (289 testów, JUnit 5 + Mockito):**

| Pakiet | Testy | Co pokrywa |
|--------|-------|-----------|
| `domain/scoring/` | 20 | ScoringEngine, PromptBuilder |
| `domain/rules/` | 25 | BoostRule, PenalizeRule, ExcludeRule |
| `domain/exception/` | 8 | Konstruktory wyjątków domenowych |
| `domain/service/activity/` | 27 | ActivityAnalysisService, MetricsCalculator (velocity, cycle time, churn) |
| `domain/service/activity/rules/` | 30 | LargePr, QuickReview, Weekend, Night, NoReview, SelfMerge, Aggregate |
| `domain/service/project/` | 39 | ArtifactDetector (22), ProjectAnalysisService (6), Concurrency (3), Enrich (5), JSON round-trip (3) |
| `application/` | 21 | AnalyzeMrService, BrowseMrService, GetResultsService, ManageReposService |
| `adapter/out/llm/` | 24 | ClaudeCliAdapter (21), NoOpAdapter (3) |
| `adapter/out/persistence/` | 21 | JpaAnalysisResultRepository (13), JpaSavedRepositoryAdapter (8) |
| `adapter/out/provider/github/` | 47 | GitHubAdapter (14), GitHubClient (21), GitHubMapper (9), ReviewAdapter (3) |
| `adapter/in/rest/` | 22 | AnalysisController (11), DiagnosticsController (2), GlobalExceptionHandler (9) |
| `adapter/config/` | 5 | LlmConfig (2), RulesConfig (3) |

**Frontend (159 testów, Vitest + React Testing Library):**

| Plik | Testy | Co pokrywa |
|------|-------|-----------|
| Komponenty analiza PR | 61 | MrTable, MrBrowseTable, MrDetailPage, AnalysisDetailPage, AnalysisHistory, ScoreBadge, SummaryCard, RepoSelector |
| Komponenty aktywność | 41 | StatsCards, FlagsList, ActivityHeatmap, DayDrillDown, ContributorSelector, ActivityBarChart, ProductivityMetricsCards, VelocityChart, ActivityDashboardTabs |
| Komponenty projekt | 31 | AiPotentialCard (7), BddSddCards (4), ProjectPrTable (6), ProjectAnalysisPage (4), VerdictPieChart (4), Layout (2) |
| Utility | 18 | Formatowanie, daty, score calculation, null safety |

### BDD Scenarios (70) — dokumentacja zachowań

| Feature file | Scenariusze | Moduł |
|-------------|-------------|-------|
| `scoring.feature` | 9 | Reguły scoringowe (boost, penalize, exclude) |
| `scoring-null-safety.feature` | 4 | Null safety w scoringu |
| `analysis.feature` | 9 | Analiza MR end-to-end (GitHub → LLM → scoring) |
| `analysis-cache.feature` | 9 | Cache analiz, selekcja MR |
| `detailed-llm-analysis.feature` | 3 | Parsowanie LLM response (categories, oversight) |
| `browse-repos.feature` | 7 | Przeglądanie MR, zarządzanie repo |
| `provider.feature` | 5 | GitHub API contract |
| `activity-analysis.feature` | 7 | Aktywność kontrybutora, flagi |
| `activity-cache.feature` | 5 | Cache aktywności, incremental update |
| `activity-metrics.feature` | 6 | Metryki wydajności (velocity, cycle time, churn) |
| `project-analysis.feature` | 6 | Analiza projektu (AI, BDD, SDD detection) |

### Integration Tests (36) — full Spring context + MockWebServer

| Klasa | Testy | Co pokrywa |
|-------|-------|-----------|
| `IntegrationTest` | 10 | Full REST round-trip: browse → analyze → get report |
| `ActivityIntegrationTest` | 5 | Activity report z MockWebServer (detale PR, reviews) |
| `ProjectAnalysisIntegrationTest` | 7 | Project analysis REST: analyze → list → get → delete → 404 + BDD/SDD detection |
| `GitHubContractIntegrationTest` | 2 | GitHub API JSON contract (mapping correctness) |
| `GitHubErrorHandlingIntegrationTest` | 3 | Rate limit, auth error, 404 handling |
| `AnalysisIntegrationTest` | 3 | Analysis CRUD |
| `BrowseIntegrationTest` | 2 | Browse + repo save |
| `ActivityCacheEndpointsIntegrationTest` | 3 | Refresh/invalidate cache REST endpoints |
| `MrAnalizerApplicationTest` | 1 | Context startup |

### E2E Tests (18) — Playwright (Chromium headless)

| Spec file | Testy | Co pokrywa |
|-----------|-------|-----------|
| `navigation.spec.ts` | 5 | Routing, navbar, 404 |
| `analysis-dashboard.spec.ts` | 3 | Dashboard z historią, sortowanie, nawigacja do detali |
| `activity-dashboard.spec.ts` | 3 | Full activity flow: repo → kontrybutor → stats + heatmap + flagi |
| `project-analysis.spec.ts` | 4 | Project analysis: analyze → results → drill-down → saved |
| `activity-tabs.spec.ts` | 3 | Zakładki: Wydajność, Aktywność, Naruszenia |

## Co pokrywają testy — mapa funkcjonalności

| Funkcjonalność | Unit | BDD | Integration | E2E | Frontend |
|---------------|------|-----|-------------|-----|----------|
| Scoring Engine | ✅ 20 | ✅ 13 | ✅ | — | — |
| GitHub Adapter | ✅ 47 | ✅ 5 | ✅ 5 | — | — |
| LLM Adapter | ✅ 24 | ✅ 3 | — | — | — |
| Analiza MR | ✅ 8 | ✅ 9 | ✅ 10 | ✅ 3 | ✅ 61 |
| Aktywność kontrybutora | ✅ 57 | ✅ 18 | ✅ 5 | ✅ 6 | ✅ 41 |
| Analiza projektu | ✅ 39 | ✅ 6 | ✅ 7 | ✅ 4 | ✅ 31 |
| Persistence (JPA) | ✅ 21 | — | ✅ | — | — |
| REST error handling | ✅ 9 | — | ✅ 3 | — | — |

## Zasady utrzymania piramidy

Zdefiniowane w `.specify/memory/constitution.md` sekcja III-b. Każda nowa funkcjonalność MUSI dodać:

1. Unit testy domain logic
2. BDD `.feature` file (test-first)
3. Integration test REST endpoints
4. Frontend component test (Vitest)
5. E2E test (Playwright) — jeśli nowa strona
6. JPA round-trip test — jeśli nowa encja
7. Concurrency test — jeśli shared state
