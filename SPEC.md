# MR Analizer — Specyfikacja

## Cel

Aplikacja webowa analizujaca Merge Requesty (GitLab) i Pull Requesty (GitHub), oceniajaca ktore z nich moglyby zostac wykonane automatycznie przez LLM. Dwuetapowy flow: najpierw przegladanie MR/PR, potem analiza z scoringiem. Raport ze statystykami, mozliwoscia filtrowania i selekcji konkretnych MR.

## Metodologia

### SDD — Specification Driven Development

Projekt stosuje Spec Kit (identyczny wzorzec jak wyhodujSam). Workflow:

1. `/speckit.specify` — specyfikacja feature'a z user stories
2. `/speckit.plan` — plan techniczny z architektura
3. `/speckit.tasks` — breakdown na taski z zaleznoscia
4. `/speckit.analyze` — walidacja spojnosci spec/plan/tasks
5. `/speckit.implement` — implementacja wg taskow

Artefakty w `specs/###-feature-name/`: spec.md, plan.md, tasks.md, research.md, quickstart.md

### BDD — Behavior Driven Development

BDD (Behavior-Driven Development) to podejscie test-first, w ktorym testy opisuja **zachowanie systemu z perspektywy uzytkownika**, a nie wewnetrzna implementacje. BDD jest synteza i udoskonaleniem praktyk wywodzacych sie z TDD (Test-Driven Development) i ATDD (Acceptance-Test-Driven Development).

**Kluczowe zasady BDD w projekcie:**

1. **Scenariusze biznesowe, nie techniczne** — kazdy scenariusz opisuje story/feature/capability z punktu widzenia uzytkownika, unikajac szczegolow implementacyjnych
2. **Test-first** — scenariusze .feature pisane PRZED implementacja, na podstawie acceptance criteria z user stories (spec.md)
3. **Zrozumiale dla nietechnicznych** — scenariusze w formacie Given/When/Then sa czytelne dla kazdego interesariusza
4. **Zywa dokumentacja** — pliki .feature sluza jednoczesnie jako testy akceptacyjne i dokumentacja zachowan systemu
5. **Mapowanie na user stories** — kazda user story z spec.md ma odpowiadajace scenariusze .feature

**Workflow BDD:**

```
User Story (spec.md)
  → Acceptance Scenarios (Given/When/Then)
    → .feature file (Gherkin)
      → Step definitions (Java)
        → Scenariusze FAIL (red)
          → Implementacja feature'a
            → Scenariusze PASS (green)
```

**Struktura plikow:**

```
backend/src/test/resources/features/       # pliki .feature (Gherkin)
    scoring.feature                        # reguly i scoring (8 scenariuszy)
    analysis.feature                       # analiza end-to-end + LLM (7 scenariuszy)
    provider.feature                       # pobieranie MR z GitHub (3 scenariusze)
    browse-repos.feature                   # przegladanie MR + zarzadzanie repo (5 scenariuszy)
    analysis-cache.feature                 # cache analiz + selekcja MR (5 scenariuszy)

backend/src/test/java/com/mranalizer/bdd/
    CucumberTest.java                      # JUnit Platform Suite runner
    CucumberSpringConfig.java              # Spring Boot context + @MockBean
    steps/
        ScoringSteps.java                  # scoring + reguly
        AnalysisSteps.java                 # analiza end-to-end
        ProviderSteps.java                 # GitHub provider
        BrowseRepoSteps.java              # przegladanie + repo management
        AnalysisCacheSteps.java           # cache + selekcja MR
        ScenarioContext.java               # shared state miedzy step classes
        DatabaseCleanupHooks.java          # czyszczenie danych przed scenariuszem
```

## Stack technologiczny

### Backend
- **Java 17**
- **Spring Boot 3.2** (Web, Validation, Data JPA, WebFlux client)
- **Maven**
- **H2** — baza danych (file-based `./data/mranalizer`), z mozliwoscia przejscia na PostgreSQL
- **Spring Data JPA** — persystencja (AnalysisReport, AnalysisResult, SavedRepository)
- **WebClient (Spring WebFlux)** — klient HTTP do API GitLab/GitHub
- **Cucumber 7 + JUnit 5** — testy BDD (28 scenariuszy)
- **JUnit 5 + Mockito** — testy jednostkowe (57 testow)
- **MockWebServer** — mockowanie GitHub API w testach integracyjnych

### Frontend
- **React 18** + **TypeScript 5**
- **Vite 5** — bundler + dev server z proxy
- **Bootstrap 5** (React-Bootstrap) — UI components
- **Axios** — HTTP client do REST API
- **React Router v6** — routing

## Architektura heksagonalna

Dwa oddzielne moduly: backend (Spring Boot REST API, port 8083) i frontend (React SPA, dev port 3000, proxy do backend).

```
mr_analizer/
├── backend/                                 # Spring Boot REST API
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/mranalizer/
│       │   ├── MrAnalizerApplication.java
│       │   │
│       │   ├── domain/                      # RDZEN — czysta logika, zero zaleznosci
│       │   │   ├── model/
│       │   │   │   ├── MergeRequest.java    # zunifikowany MR/PR (19 pol)
│       │   │   │   ├── ChangedFile.java     # record
│       │   │   │   ├── DiffStats.java       # record
│       │   │   │   ├── AnalysisResult.java  # score + verdict + reasons
│       │   │   │   ├── AnalysisReport.java  # raport zbiorczy z counts
│       │   │   │   ├── Verdict.java         # enum: AUTOMATABLE, MAYBE, NOT_SUITABLE
│       │   │   │   ├── FetchCriteria.java   # kryteria pobierania MR
│       │   │   │   ├── LlmAssessment.java   # record: scoreAdjustment + comment
│       │   │   │   ├── RuleResult.java      # record: ruleName + matched + weight + reason
│       │   │   │   └── SavedRepository.java # zapisane repozytorium
│       │   │   ├── exception/
│       │   │   │   ├── ProviderException.java          # bazowy wyjatek providera
│       │   │   │   ├── ProviderRateLimitException.java # rate limit (extends ProviderException)
│       │   │   │   ├── ProviderAuthException.java      # auth error (extends ProviderException)
│       │   │   │   ├── ReportNotFoundException.java    # brak raportu/wyniku
│       │   │   │   └── InvalidRequestException.java    # walidacja inputu
│       │   │   ├── rules/
│       │   │   │   ├── Rule.java            # interfejs reguly
│       │   │   │   ├── ExcludeRule.java     # factory: byLabels, byMinFiles, byMaxFiles, byExtensions
│       │   │   │   ├── BoostRule.java       # factory: byKeywords, byHasTests, byFilesRange, byLabels
│       │   │   │   └── PenalizeRule.java    # factory: byLargeDiff, byNoDescription, byTouchesConfig
│       │   │   ├── scoring/
│       │   │   │   ├── ScoringEngine.java   # evaluates rules → score → verdict
│       │   │   │   └── ScoringConfig.java   # baseScore, thresholds
│       │   │   └── port/
│       │   │       ├── in/
│       │   │       │   ├── AnalyzeMrUseCase.java          # analyze + deleteAnalysis
│       │   │       │   ├── GetAnalysisResultsUseCase.java # getAll, getReport, getResult
│       │   │       │   ├── BrowseMrUseCase.java           # browse MR bez analizy
│       │   │       │   └── ManageReposUseCase.java        # CRUD saved repos
│       │   │       └── out/
│       │   │           ├── MergeRequestProvider.java      # port: GitHub/GitLab
│       │   │           ├── LlmAnalyzer.java               # port: Claude CLI/NoOp
│       │   │           ├── AnalysisResultRepository.java  # port: persystencja analiz
│       │   │           └── SavedRepositoryPort.java       # port: persystencja repo
│       │   │
│       │   ├── domain/model/activity/         # modele modulu aktywnosci
│       │   │   ├── Severity.java, FlagType.java, ActivityFlag.java
│       │   │   ├── ContributorStats.java, DailyActivity.java
│       │   │   └── ActivityReport.java, ContributorInfo.java
│       │   ├── domain/port/in/activity/
│       │   │   └── ActivityAnalysisUseCase.java
│       │   ├── domain/port/out/activity/
│       │   │   └── ReviewProvider.java
│       │   ├── domain/service/activity/
│       │   │   ├── ActivityAnalysisService.java
│       │   │   ├── DateTimeUtils.java
│       │   │   └── rules/ (ActivityRule, LargePrRule, QuickReviewRule, WeekendWorkRule,
│       │   │               NightWorkRule, NoReviewRule, SelfMergeRule, AggregateRules)
│       │   │
│       │   ├── application/                 # USE CASES — orkiestracja
│       │   │   ├── AnalyzeMrService.java    # analiza + cache detection + selekcja MR (implements AnalyzeMrUseCase)
│       │   │   ├── GetAnalysisResultsService.java  # query: getAll, getReport, getResult (implements GetAnalysisResultsUseCase)
│       │   │   ├── BrowseMrService.java     # pobieranie MR bez scoringu
│       │   │   ├── ManageReposService.java  # CRUD saved repos
│       │   │   └── dto/
│       │   │       ├── AnalysisRequestDto.java  # incl. selectedMrIds
│       │   │       └── AnalysisSummaryDto.java
│       │   │
│       │   └── adapter/                     # ADAPTERY
│       │       ├── in/rest/
│       │       │   ├── AnalysisRestController.java  # POST/GET/DELETE /api/analysis
│       │       │   ├── BrowseRestController.java    # POST /api/browse
│       │       │   ├── RepoRestController.java      # GET/POST/DELETE /api/repos
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   └── dto/
│       │       │       ├── AnalysisResponse.java
│       │       │       ├── MrDetailResponse.java
│       │       │       ├── MrBrowseResponse.java
│       │       │       ├── SavedRepoResponse.java
│       │       │       └── ErrorResponse.java
│       │       ├── out/
│       │       │   ├── provider/github/
│       │       │   │   ├── GitHubAdapter.java       # impl MergeRequestProvider
│       │       │   │   ├── GitHubClient.java        # WebClient + pagination + rate limit
│       │       │   │   ├── GitHubMapper.java        # GitHub DTO → domain
│       │       │   │   ├── RateLimitException.java
│       │       │   │   └── dto/
│       │       │   │       ├── GitHubPullRequest.java
│       │       │   │       └── GitHubFile.java
│       │       │   ├── llm/
│       │       │   │   ├── ClaudeCliAdapter.java    # ProcessBuilder + timeout + fallback
│       │       │   │   └── NoOpLlmAdapter.java
│       │       │   └── persistence/
│       │       │       ├── JpaAnalysisResultRepository.java
│       │       │       ├── InMemoryAnalysisResultRepository.java  # @Profile("test")
│       │       │       ├── JpaSavedRepositoryAdapter.java
│       │       │       ├── InMemorySavedRepositoryAdapter.java    # @Profile("test")
│       │       │       ├── SpringDataAnalysisResultRepository.java
│       │       │       ├── SpringDataSavedRepoRepository.java
│       │       │       └── entity/
│       │       │           ├── AnalysisReportEntity.java
│       │       │           ├── AnalysisResultEntity.java
│       │       │           └── SavedRepositoryEntity.java
│       │       └── config/
│       │           ├── ProviderConfig.java
│       │           ├── LlmConfig.java
│       │           ├── CorsConfig.java
│       │           └── RulesConfig.java
│       └── main/resources/
│           └── application.yml
│
├── frontend/                                # React + TypeScript SPA
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx                          # React Router: /, /mr/:reportId/:resultId
│       ├── api/
│       │   └── analysisApi.ts              # Axios: analysis, browse, repos, delete
│       ├── components/
│       │   ├── Layout.tsx                  # navbar + Outlet
│       │   ├── RepoSelector.tsx            # dropdown saved repos + manual input
│       │   ├── MrBrowseTable.tsx           # tabela MR z checkboxami (bez scoringu)
│       │   ├── MrTable.tsx                 # tabela wynikow ze scoringiem
│       │   ├── AnalysisForm.tsx            # formularz (branch, daty, limit, LLM)
│       │   ├── AnalysisHistory.tsx         # historia analiz z przyciskiem usun
│       │   ├── ScoreBadge.tsx              # kolorowy badge score/verdict
│       │   └── SummaryCard.tsx             # 3 karty: automatable/maybe/not suitable
│       ├── pages/
│       │   ├── DashboardPage.tsx           # dwuetapowy flow: browse → analyze
│       │   ├── MrDetailPage.tsx            # szczegoly MR + score breakdown (opcjonalny)
│       │   └── ActivityDashboardPage.tsx   # dashboard aktywnosci kontrybutora
│       ├── components/activity/
│       │   ├── ContributorSelector.tsx     # dropdown kontrybutora
│       │   ├── StatsCards.tsx              # karty statystyk + klikalne filtry severity
│       │   ├── FlagsList.tsx               # tabela flag z filtrami
│       │   ├── ActivityHeatmap.tsx         # SVG heatmapa (GitHub-style)
│       │   └── DayDrillDown.tsx            # drill-down PR po kliknieciu dnia
│       ├── types/
│       │   ├── index.ts                    # TypeScript interfaces
│       │   └── activity.ts                 # typy modulu aktywnosci
│       └── styles/
│           └── app.css                     # verdict colors, responsive
│
├── specs/                                   # SDD feature specs
│   ├── 001-mvp-core/                       # DONE — MVP
│   └── 002-mr-browse-analyze/              # DONE — browse + repos + cache + selekcja
├── .specify/                                # Spec Kit infrastructure
├── .claude/commands/                        # speckit commands
├── CLAUDE.md
├── SPEC.md
└── .gitignore
```

## REST API

```
POST   /api/browse                   # pobierz MR/PR bez analizy (body: criteria)
POST   /api/analysis                 # uruchom analize (body: criteria + selectedMrIds)
GET    /api/analysis                 # lista raportow analiz (?projectSlug= filtr)
GET    /api/analysis/{reportId}      # szczegoly raportu z wynikami
DELETE /api/analysis/{reportId}      # usun zapisana analize
GET    /api/analysis/{reportId}/mrs/{resultId}  # szczegoly MR + score breakdown
GET    /api/summary/{reportId}       # podsumowanie (counts per verdict)
GET    /api/repos                    # lista zapisanych repozytoriow
POST   /api/repos                    # dodaj repo (body: projectSlug, provider)
DELETE /api/repos/{id}               # usun repo z listy

# Activity (odseparowany modul)
GET    /api/activity/{owner}/{repo}/contributors      # lista kontrybutorów repo
GET    /api/activity/{owner}/{repo}/report?author=     # raport aktywnosci (flagi, stats, metryki wydajnosci)
POST   /api/activity/{owner}/{repo}/refresh           # incremental cache update
DELETE /api/activity/{owner}/{repo}/cache              # full cache invalidation
```

## GUI — flow uzytkownika

### Dwuetapowy flow (zaimplementowany)

1. **Wybor repo** — RepoSelector: dropdown zapisanych repozytoriow + pole na nowy slug
2. **Pobierz MR** — formularz (branch, daty, limit) → klik "Pobierz MR" → MrBrowseTable (lista MR bez scoringu, z checkboxami do selekcji)
3. **Analizuj** — zaznacz MR checkboxami → klik "Analizuj (N z M)" → SummaryCard + MrTable z kolorami i scorami
4. **Szczegoly MR** — klik na wiersz → nowa karta z MrDetailPage (dane MR + score breakdown jesli analiza istnieje)
5. **Cache** — jesli analiza juz istnieje, badge "Analiza istnieje" z opcja wczytaj/usun
6. **Historia** — AnalysisHistory na dole dashboardu: lista analiz z przyciskiem "Usun"

### Faza 2 — Rozszerzenie (planowane)

- Wykresy (Chart.js): rozklad score, trend w czasie, top autorzy
- Filtrowanie: po autorze, repo, dacie, verdict
- Wyszukiwarka MR
- Porownanie repozytoriow

### Faza 3 — Zaawansowane (planowane)

- Dashboard z widgetami (statystyki per zespol/osoba)
- Eksport do CSV/PDF
- Tryb CI — webhook, automatyczna analiza nowych MR
- Konfiguracja regul przez GUI
- Adapter GitLab
- Adapter LLM przez API (Claude API, OpenAI API)

## System regul i scoring

| Kategoria | Efekt na score |
|-----------|---------------|
| `exclude` | score = 0, verdict = NOT_SUITABLE |
| `boost` | score += weight (np. +0.15) |
| `penalize` | score -= weight (np. -0.2) |

Scoring: base score 0.5 + sum(boost weights) - sum(penalize weights) + LLM adjustment. Clamped to [0.0, 1.0]. Verdict thresholds: >= 0.7 AUTOMATABLE, >= 0.4 MAYBE, < 0.4 NOT_SUITABLE.

Konfiguracja regul w `application.yml` (exclude labels, min/max files, file extensions, boost keywords, penalize large diff itp.)

## Porty aplikacji

- **Backend**: 8083 (Spring Boot REST API)
- **Frontend dev**: 3000 (Vite dev server, proxy do 8083)
- **H2 Console**: 8083/h2-console

## Testy

- **472 testow, 0 failures** (340 backend + 132 frontend)
- 64 scenariusze BDD (Cucumber/Gherkin) w 10 plikach .feature
- 276 testow jednostkowych i integracyjnych (JUnit 5 + Mockito)
- 132 testow frontend (Vitest + React Testing Library)
- Testy integracyjne: Spring Boot + H2 + MockBean provider

## Zrealizowane features

### 001-mvp-core (DONE)
- Model domenowy, silnik regul, scoring
- Adapter GitHub (WebClient, paginacja, rate limit)
- Adapter Claude CLI + NoOp
- REST API (5 endpointow)
- React dashboard z formularzem i tabela wynikow
- Persystencja H2 (JPA)
- 60 testow (18 BDD + 42 unit/integration)

### 002-mr-browse-analyze (DONE)
- Dwuetapowy flow: browse MR → analyze
- Dropdown zapisanych repozytoriow (CRUD)
- Cache analiz + usuwanie
- Selekcja MR checkboxami przed analiza
- MrBrowseTable (lista MR bez scoringu)
- AnalysisHistory (historia analiz z usuwaniem)
- 25 nowych testow (10 BDD + 15 unit/integration)

### 003-cleanup-architecture-robustness (DONE)

Sprint sprzatajacy — architektura i odpornosc (SDD/BDD code review → fix):

**Architektura:**
- Rozbicie god class: AnalyzeMrService → AnalyzeMrService (command) + GetAnalysisResultsService (query)
- Domain exceptions: ProviderException/RateLimitException/AuthException, ReportNotFoundException, InvalidRequestException
- Usuniety adapter-level RateLimitException — wyjatki zyja w domain
- GlobalExceptionHandler: konkretne handlery per exception typ (zamiast string matching na RuntimeException)
- DRY: AnalysisRequestDto.toFetchCriteria() zamiast duplikacji w controllerach
- Controller zalezy od portow (interfejsow), nie od konkretnych serwisow

**Robustnosc:**
- NPE fix: MrDetailResponse.findReasonForRule() — null check na elementach streama
- Null safety: ScoringEngine — null llmComment → "no comment"
- Walidacja: projectSlug required w AnalysisRequestDto + service-level validation
- HttpMessageNotReadableException handler → 400 zamiast 500

**Testy:** +4 testy (1 BDD scenariusz + 5 unit testow w GetAnalysisResultsServiceTest - 2 przeniesione)

### 012-performance-profiling (DONE)

Narzedzie deweloperskie do profilowania wydajnosci:
- Komenda Claude `/profile` — orkiestruje profilowanie i generuje rekomendacje
- `scripts/profile.sh` — skrypt zbierajacy metryki (async-profiler, Actuator, Hibernate Stats)
- `DiagnosticsController` (`@Profile("dev")`) — endpoint SQL stats z Hibernate Statistics
- Spring Boot Actuator + Micrometer — metryki HTTP/JVM/GC
- async-profiler v3.0 — CPU/alloc flame graphs (zewnetrzne narzedzie CLI)
- Raporty Markdown z flame graphs w `reports/` (gitignored)
- +2 testy (unit DiagnosticsController)

### 015-user-activity-health (DONE)

Odseparowany modul analizy aktywnosci kontrybutora — wykrywanie nieprawidlowosci w procesie wytwórczym:

**Reguly wykrywania (Strategy pattern):**
- `LargePrRule` — PR >500 linii (warning), >1000 (critical)
- `QuickReviewRule` — merge <10 min przy >50 liniach (warning), <5 min przy >100 (critical)
- `WeekendWorkRule` — PR utworzony/zmergowany w weekend
- `NightWorkRule` — PR utworzony/zmergowany 22:00-06:00
- `NoReviewRule` — zmergowany PR bez zadnego review
- `SelfMergeRule` — PR zatwierdzony tylko przez autora
- `AggregateRules` — >30% PR weekendowych → warning

**Frontend:**
- Dashboard z selektorem kontrybutora, kartami statystyk
- Heatmapa SVG (GitHub contribution graph style) — 13 tygodni, 5 poziomow koloru, tooltip, drill-down
- Klikalne badge severity (filtr tabeli), dropdown kategorii
- Route: `/activity` i `/activity/:owner/:repo`

**Testy:** 36 unit testow regul + 7 BDD scenariuszy + 8 testow frontend (Vitest)

### 020-activity-cache-velocity (DONE)

Incremental cache per-repo + metryki wydajnosci kontrybutora:

**Cache:**
- In-memory cache (`ActivityRepoCache`) per-repo z TTL 15 min
- Cold start: full fetch (all PRs + details + reviews for ALL authors)
- Incremental update: `fetchMergeRequestsUpdatedSince` — tylko zmienione PRs
- Switching between contributors = 0 API calls (filter locally from cache)
- REST: `POST /refresh` (incremental), `DELETE /cache` (full invalidation)

**Metryki wydajnosci** (`MetricsCalculator`, `ProductivityMetrics`):
- Velocity: PRs/tydzien (4-week window), weekly breakdown, trend (rising/falling/stable)
- Cycle Time: avg/median/p90 godzin od created do merged
- Development Impact: total additions/deletions, avg per PR, add/delete ratio
- Code Churn: deletions/additions ratio, label (nowy kod / zbalansowany / refactoring)
- Review Engagement: reviews given/received, ratio, label

**Provider abstraction:**
- `MergeRequest.updatedAt` — nowe pole (nullable, backward-compatible)
- `MergeRequestProvider.fetchMergeRequestsUpdatedSince()` — nowa metoda na porcie
- GitHub: `sort=updated&direction=desc`, stop when updatedAt < since
- GitLab (ready): `order_by=updated_at&updated_after=<ISO>` — identyczna semantyka, inne nazwy

**Frontend:** `ProductivityMetricsCards` (velocity, cycle time, impact, churn, review engagement), `VelocityChart` (SVG sparkline), przycisk "Odswież dane"

**Testy:** 19 unit MetricsCalculator + 8 unit ActivityAnalysisService (cache) + 11 BDD scenariuszy (cache + metryki) + 10 frontend (Vitest)

## Uwagi

- Tokeny API wylacznie przez zmienne srodowiskowe (`export GITHUB_TOKEN=...`)
- Rate limiting — respektowanie limitow API providerow
- Paginacja — automatyczne pobieranie wszystkich stron (Link header)
- Claude CLI adapter: `claude -p "prompt"` — timeout 60s, fallback na NoOp
- H2 file-based (`./data/mranalizer`) — dane przetrwaja restart
- Hexagonalna architektura: domain (0 deps) → ports → adapters (Spring, JPA, WebClient)
- CORS skonfigurowany dla dev (localhost:3000 → localhost:8083)
