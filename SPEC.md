# MR Analizer — Specyfikacja

## Cel

Aplikacja webowa analizujaca Merge Requesty (GitLab) i Pull Requesty (GitHub), oceniajaca ktore z nich moglyby zostac wykonane automatycznie przez LLM. Raport ze scoringiem, statystykami i mozliwoscia filtrowania.

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

Testy akceptacyjne w Cucumber (Gherkin `.feature` files) weryfikuja scenariusze z user stories.
Struktura:

```
backend/src/test/resources/features/          # pliki .feature (Gherkin)
backend/src/test/java/com/mranalizer/bdd/
    CucumberTestRunner.java                   # JUnit runner
    steps/                                    # step definitions
        AnalysisSteps.java
        ProviderSteps.java
        ScoringSteps.java
```

Przyklady scenariuszy:

```gherkin
Feature: Analiza MR pod katem automatyzacji

  Scenario: MR z refaktoringiem dostaje wysoki score
    Given repozytorium "owner/repo" z providerem "github"
    And MR #42 z tytulem "Refactor user service"
    And MR ma 5 zmienionych plikow
    And MR ma testy
    When uruchamiam analize MR #42
    Then score powinien byc wiekszy niz 0.7
    And verdict powinien byc "AUTOMATABLE"

  Scenario: MR hotfix jest wykluczony
    Given MR #99 z labelem "hotfix"
    When uruchamiam analize MR #99
    Then verdict powinien byc "NOT_SUITABLE"
    And reason powinien zawierac "excluded by label rule"
```

## Stack technologiczny

### Backend
- **Java 17**
- **Spring Boot 3.x** (Web, Validation)
- **Maven**
- **H2** — baza danych (dev), z mozliwoscia przejscia na PostgreSQL
- **Spring Data JPA** — persystencja
- **WebClient (Spring WebFlux)** — klient HTTP do API GitLab/GitHub
- **Cucumber + JUnit 5** — testy BDD
- **JUnit 5 + Mockito** — testy jednostkowe
- **Lombok** — redukcja boilerplate'u

### Frontend
- **React 18** + **TypeScript**
- **Vite** — bundler
- **Bootstrap 5** (React-Bootstrap) — UI components
- **Chart.js** (react-chartjs-2) — wykresy (Faza 2+)
- **Axios** — HTTP client do REST API
- **React Router** — routing

## Architektura

Dwa oddzielne moduly: backend (Spring Boot REST API, port 8083) i frontend (React SPA, dev port 3000, w produkcji serwowany przez backend jako static resources).

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
│       │   │   │   ├── MergeRequest.java
│       │   │   │   ├── ChangedFile.java
│       │   │   │   ├── DiffStats.java
│       │   │   │   ├── AnalysisResult.java
│       │   │   │   ├── Verdict.java         # enum: AUTOMATABLE, MAYBE, NOT_SUITABLE
│       │   │   │   └── AnalysisReport.java
│       │   │   ├── rules/
│       │   │   │   ├── Rule.java
│       │   │   │   ├── RuleResult.java
│       │   │   │   ├── ExcludeRule.java
│       │   │   │   ├── BoostRule.java
│       │   │   │   └── PenalizeRule.java
│       │   │   ├── scoring/
│       │   │   │   ├── ScoringEngine.java
│       │   │   │   └── ScoringConfig.java
│       │   │   └── port/
│       │   │       ├── in/
│       │   │       │   ├── AnalyzeMrUseCase.java
│       │   │       │   ├── GetAnalysisResultsUseCase.java
│       │   │       │   └── ManageRulesUseCase.java
│       │   │       └── out/
│       │   │           ├── MergeRequestProvider.java
│       │   │           ├── LlmAnalyzer.java
│       │   │           └── AnalysisResultRepository.java
│       │   │
│       │   ├── application/                 # USE CASES — orkiestracja
│       │   │   ├── AnalyzeMrService.java
│       │   │   └── dto/
│       │   │       ├── AnalysisRequestDto.java
│       │   │       └── AnalysisSummaryDto.java
│       │   │
│       │   └── adapter/                     # ADAPTERY
│       │       ├── in/
│       │       │   └── rest/
│       │       │       ├── AnalysisRestController.java   # REST API
│       │       │       └── dto/
│       │       │           ├── AnalysisResponse.java
│       │       │           └── MrDetailResponse.java
│       │       ├── out/
│       │       │   ├── provider/
│       │       │   │   ├── github/
│       │       │   │   │   ├── GitHubAdapter.java
│       │       │   │   │   ├── GitHubClient.java
│       │       │   │   │   └── GitHubMapper.java
│       │       │   │   └── gitlab/
│       │       │   │       ├── GitLabAdapter.java
│       │       │   │       ├── GitLabClient.java
│       │       │   │       └── GitLabMapper.java
│       │       │   ├── llm/
│       │       │   │   ├── ClaudeCliAdapter.java
│       │       │   │   └── NoOpLlmAdapter.java
│       │       │   └── persistence/
│       │       │       ├── JpaAnalysisResultRepository.java
│       │       │       └── entity/
│       │       │           └── AnalysisResultEntity.java
│       │       └── config/
│       │           ├── ProviderConfig.java
│       │           ├── LlmConfig.java
│       │           └── CorsConfig.java
│       └── main/resources/
│           └── application.yml
│
├── frontend/                                # React + TypeScript SPA
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── api/
│       │   └── analysisApi.ts               # Axios client do REST API
│       ├── components/
│       │   ├── Layout.tsx                   # navbar + layout
│       │   ├── AnalysisForm.tsx             # formularz uruchomienia analizy
│       │   ├── MrTable.tsx                  # tabela wynikow
│       │   ├── ScoreBadge.tsx               # kolorowy badge score/verdict
│       │   └── SummaryCard.tsx              # podsumowanie (automatable/maybe/not)
│       ├── pages/
│       │   ├── DashboardPage.tsx            # glowny widok
│       │   └── MrDetailPage.tsx             # szczegoly MR + score breakdown
│       ├── types/
│       │   └── index.ts                     # TypeScript interfaces (MergeRequest, AnalysisResult, etc.)
│       └── styles/
│           └── app.css
│
├── specs/                                   # SDD feature specs
├── .specify/                                # Spec Kit infrastructure
├── .claude/commands/                        # speckit commands
├── CLAUDE.md
├── SPEC.md
└── .gitignore
```

## REST API (backend)

```
POST   /api/analysis              # uruchom analize (body: provider, projectSlug, branch, dateRange, limit)
GET    /api/analysis              # lista wynikow analiz
GET    /api/analysis/{id}         # szczegoly analizy
GET    /api/analysis/{id}/mrs     # lista MR z wynikami dla danej analizy
GET    /api/mrs/{id}              # szczegoly MR + score breakdown
GET    /api/summary               # podsumowanie (counts per verdict)
```

## Model domenowy

### MergeRequest (zunifikowany)

```java
public class MergeRequest {
    private Long id;
    private String externalId;         // numer MR/PR u providera
    private String title;
    private String description;
    private String author;
    private String sourceBranch;
    private String targetBranch;
    private String state;              // merged, closed, open
    private LocalDateTime createdAt;
    private LocalDateTime mergedAt;
    private List<String> labels;
    private List<ChangedFile> changedFiles;
    private DiffStats diffStats;
    private boolean hasTests;
    private boolean ciPassed;
    private int approvalsCount;
    private int commentsCount;
    private String provider;           // "github" / "gitlab"
    private String url;
    private String projectSlug;        // owner/repo
}
```

### AnalysisResult

```java
public class AnalysisResult {
    private Long id;
    private MergeRequest mergeRequest;
    private double score;              // 0.0 - 1.0
    private Verdict verdict;           // AUTOMATABLE, MAYBE, NOT_SUITABLE
    private List<String> reasons;
    private List<String> matchedRules;
    private String llmComment;         // opcjonalny komentarz z LLM
    private LocalDateTime analyzedAt;
}
```

## Porty (interfejsy)

### MergeRequestProvider (port wyjsciowy)

```java
public interface MergeRequestProvider {
    List<MergeRequest> fetchMergeRequests(FetchCriteria criteria);
    MergeRequest fetchMergeRequest(String projectSlug, String mrId);
    String getProviderName();  // "github" / "gitlab"
}
```

### LlmAnalyzer (port wyjsciowy)

```java
public interface LlmAnalyzer {
    LlmAssessment analyze(MergeRequest mr);
    String getProviderName();  // "claude-cli", "openai", "none"
}
```

Implementacja `ClaudeCliAdapter` — wywoluje `claude -p "prompt"` jako process, parsuje odpowiedz. Dziala w ramach istniejacej subskrypcji, zero kosztow API.

## System regul i scoring

| Kategoria | Efekt na score |
|-----------|---------------|
| `exclude` | score = 0, verdict = NOT_SUITABLE |
| `boost` | score += weight (np. +0.15) |
| `penalize` | score -= weight (np. -0.2) |

**Konfiguracja w `application.yml`:**

```yaml
mr-analizer:
  provider: github  # lub gitlab
  github:
    token: ${GITHUB_TOKEN}
    api-url: https://api.github.com
  gitlab:
    token: ${GITLAB_TOKEN}
    api-url: https://gitlab.example.com/api/v4

  llm:
    adapter: claude-cli  # claude-cli | none
    claude-cli:
      command: claude
      timeout-seconds: 60

  scoring:
    base-score: 0.5
    automatable-threshold: 0.7
    maybe-threshold: 0.4

  rules:
    exclude:
      min-changed-files: 2
      max-changed-files: 50
      file-extensions-only: [".env", ".yml", ".toml", ".lock"]
      labels: ["hotfix", "security", "emergency"]
    boost:
      description-keywords:
        words: ["refactor", "cleanup", "add test", "rename"]
        weight: 0.2
      has-tests:
        weight: 0.15
      changed-files-range:
        min: 3
        max: 15
        weight: 0.1
      labels:
        values: ["tech-debt", "refactoring", "chore"]
        weight: 0.15
    penalize:
      large-diff:
        threshold: 500
        weight: 0.2
      no-description:
        weight: 0.3
      touches-config:
        weight: 0.1
```

## GUI — widoki (React)

### Faza 1 — MVP

**DashboardPage (`/`)**
- AnalysisForm: wybor providera, project slug, branch, zakres dat, limit
- Przycisk "Analizuj"
- MrTable: #, tytul, autor, score, verdict, link
- Kolorowanie wierszy: zielony (automatable), zolty (maybe), czerwony (not suitable)
- SummaryCard: liczba MR w kazdej kategorii

**MrDetailPage (`/mr/:id`)**
- Pelne dane MR
- Score breakdown — ktore reguly zadzialaly i z jakim efektem
- Komentarz LLM (jesli wlaczony)

### Faza 2 — Rozszerzenie

- Wykresy (Chart.js / react-chartjs-2): rozklad score, trend w czasie, top autorzy
- Filtrowanie: po autorze, repo, dacie, verdict
- Wyszukiwarka MR
- Porownanie repozytoriow
- Historia analiz

### Faza 3 — Zaawansowane

- Dashboard z widgetami (statystyki per zespol/osoba)
- Eksport do CSV/PDF
- Tryb CI — webhook, automatyczna analiza nowych MR
- Konfiguracja regul przez GUI

## Porty aplikacji

- **Backend**: 8083 (Spring Boot REST API)
- **Frontend dev**: 3000 (Vite dev server, proxy do 8083)
- **Produkcja**: frontend buildowany do static, serwowany przez backend na 8083

## Fazy realizacji

### Faza 1 — MVP
- [ ] Inicjalizacja projektu Spring Boot (Maven, Java 17) — backend/
- [ ] Inicjalizacja React + TypeScript (Vite) — frontend/
- [ ] Model domenowy (MergeRequest, AnalysisResult, reguly)
- [ ] Port MergeRequestProvider + adapter GitHub
- [ ] Silnik regul i scoring
- [ ] Port LlmAnalyzer + adapter Claude CLI
- [ ] Adapter NoOp LLM (analiza tylko regulami)
- [ ] Persystencja H2 + JPA
- [ ] REST API (endpointy analizy)
- [ ] Frontend: DashboardPage + AnalysisForm + MrTable + MrDetailPage
- [ ] Konfiguracja przez application.yml
- [ ] Testy BDD (Cucumber .feature files)
- [ ] Testy jednostkowe (domain, rules, scoring)

### Faza 2 — Rozszerzenie
- [ ] Adapter GitLab
- [ ] Wykresy i statystyki (Chart.js)
- [ ] Filtrowanie i wyszukiwanie (po autorze, repo, dacie)
- [ ] Historia analiz z porownywaniem
- [ ] Cache wynikow API

### Faza 3 — Zaawansowane
- [ ] Analiza tresci diffa heurystykami (bez LLM)
- [ ] Adapter LLM przez API (Claude API, OpenAI API)
- [ ] Konfiguracja regul przez GUI
- [ ] Eksport CSV/PDF
- [ ] Tryb CI / webhook

## Uwagi

- Tokeny API wylacznie przez zmienne srodowiskowe
- Rate limiting — respektowanie limitow API providerow
- Paginacja — automatyczne pobieranie wszystkich stron
- Claude CLI adapter: `claude -p "prompt"` — timeout 60s, parsowanie stdout
- Hexagonalna architektura umozliwia latwe dodanie nowego providera lub LLM bez zmian w logice
- CORS skonfigurowany w CorsConfig dla dev (localhost:3000 → localhost:8083)
