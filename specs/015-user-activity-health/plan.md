# 015: Plan implementacji — Aktywność użytkownika + wykrywanie nieprawidłowości

**Branch**: `015-user-activity-health` | **Date**: 2026-03-22 | **Spec**: `specs/015-user-activity-health/spec.md`

## Summary

Nowy, odseparowany moduł analizujący aktywność kontrybutora w repozytorium GitHub. Wykrywa nieprawidłowości (za duże PR-y, rubber-stamping, praca weekendowa, brak review) i prezentuje je na dedykowanym dashboardzie z heatmapą aktywności w stylu GitHub.

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.x (backend), React 18 + TypeScript (frontend)
**Primary Dependencies**: Spring Web, WebFlux client (GitHub API), React-Bootstrap, SVG (heatmapa)
**Storage**: Brak persystencji — raporty generowane on-demand (in-memory)
**Testing**: JUnit 5 + Mockito (unit), Cucumber 7 (BDD), Vitest + RTL (frontend)
**Performance Goals**: Dashboard <5s dla repo z 200 PR-ami
**Constraints**: Odseparowany od istniejącego modułu analizy LLM

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | OK | Osobny pakiet `activity/` w domain, porty, adaptery |
| II. Provider Abstraction | OK | Reużywa `MergeRequestProvider`, nowy port `ReviewProvider` |
| III. BDD Testing | OK | Feature files PRZED implementacją, scenariusze w spec |
| IV. SDD Workflow | OK | spec.md → plan.md → tasks.md → implement |
| V. Simplicity (YAGNI) | OK | Brak persystencji, brak konfiguracji progów przez UI, brak timezone |

## Project Structure

### Source Code

```text
backend/src/main/java/com/mranalizer/
  domain/
    model/activity/
      ActivityReport.java         # Wynik analizy: contributor, repo, flags, stats, dailyActivity
      ActivityFlag.java           # Record: type, severity, description, prReference
      ContributorStats.java       # Record: totalPrs, avgSize, avgReviewTime, weekendPct
      DailyActivity.java          # Record: date, count, prSummaries
      Severity.java               # Enum: CRITICAL, WARNING, INFO
      FlagType.java               # Enum: LARGE_PR, VERY_LARGE_PR, QUICK_REVIEW, ...
    port/
      in/activity/
        ActivityAnalysisUseCase.java    # getContributors(), analyzeActivity()
      out/activity/
        ReviewProvider.java             # fetchReviews(projectSlug, prNumber)
    service/activity/
      ActivityAnalysisService.java      # Orkiestracja: fetch PR-ów → apply rules → build report
      rules/
        ActivityRule.java               # Interface: List<ActivityFlag> evaluate(MergeRequest, ReviewInfo)
        LargePrRule.java                # >500 warning, >1000 critical
        QuickReviewRule.java            # Czas review vs rozmiar
        WeekendWorkRule.java            # Dzień tygodnia created/merged
        NightWorkRule.java              # Godzina 22-06
        NoReviewRule.java               # 0 reviews
        SelfMergeRule.java              # Autor = merger
        AggregateRules.java             # Reguły na poziomie całego raportu (np. % weekendowe >30%)
  adapter/
    in/web/activity/
      ActivityController.java           # /api/activity/{owner}/{repo}/**
      dto/
        ContributorResponse.java
        ActivityReportResponse.java
    out/provider/github/
      GitHubClient.java                 # +fetchReviews() method
      GitHubReviewAdapter.java          # Implements ReviewProvider
      dto/
        GitHubReview.java               # DTO: user, state, submitted_at

frontend/src/
  pages/
    ActivityDashboardPage.tsx           # Główna strona aktywności
  components/activity/
    ContributorSelector.tsx             # Dropdown/lista kontrybutorów
    StatsCards.tsx                       # Karty ze statystykami
    FlagsList.tsx                        # Lista flag z severity badges
    ActivityHeatmap.tsx                  # SVG heatmapa (GitHub-style)
    DayDrillDown.tsx                    # Lista PR-ów po kliknięciu dnia
  services/
    activityApi.ts                      # API calls do /api/activity/**
  types/
    activity.ts                         # TypeScript typy

backend/src/test/
  java/com/mranalizer/
    bdd/steps/
      ActivityAnalysisSteps.java
    domain/service/activity/
      ActivityAnalysisServiceTest.java
      rules/
        LargePrRuleTest.java
        QuickReviewRuleTest.java
        WeekendWorkRuleTest.java
        NightWorkRuleTest.java
        NoReviewRuleTest.java
        SelfMergeRuleTest.java
        AggregateRulesTest.java
    adapter/out/provider/github/
      GitHubReviewAdapterTest.java
  resources/features/
    activity-analysis.feature
```

## Warstwy zmian (od wewnątrz hexagonu na zewnątrz)

### Warstwa 1: Domain models (pure Java, 0 deps)

1. **`Severity` enum** — `CRITICAL`, `WARNING`, `INFO` z natural ordering (critical first)

2. **`FlagType` enum** — `LARGE_PR`, `VERY_LARGE_PR`, `QUICK_REVIEW`, `SUSPICIOUS_QUICK_MERGE`, `WEEKEND_WORK`, `NIGHT_WORK`, `NO_REVIEW`, `SELF_MERGE`, `HIGH_WEEKEND_RATIO`

3. **`ActivityFlag` record** — `(FlagType type, Severity severity, String description, String prReference)`

4. **`ContributorStats` record** — `(int totalPrs, double avgSize, double avgReviewTimeMinutes, double weekendPercentage, Map<Severity, Long> flagCountBySeverity)`

5. **`DailyActivity` record** — `(LocalDate date, int count, List<PrSummary> pullRequests)` z nested `PrSummary(String id, String title, int size, List<ActivityFlag> flags)`

6. **`ActivityReport`** — klasa z polami: `contributor`, `projectSlug`, `stats`, `List<ActivityFlag> flags`, `Map<LocalDate, DailyActivity> dailyActivity`, `List<MergeRequest> pullRequests`

### Warstwa 2: Domain ports

7. **`ActivityAnalysisUseCase`** (port in) — interfejs:
   - `List<ContributorInfo> getContributors(String projectSlug)` — unikalni autorzy PR-ów
   - `ActivityReport analyzeActivity(String projectSlug, String author)` — pełna analiza

8. **`ReviewProvider`** (port out) — interfejs:
   - `List<ReviewInfo> fetchReviews(String projectSlug, int prNumber)` — reviews z GitHub
   - `ReviewInfo` record: `(String reviewer, String state, LocalDateTime submittedAt)`

### Warstwa 3: Domain service + rules (Strategy pattern)

9. **`ActivityRule` interface** — `List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews)`

10. **Implementacje reguł** (każda osobna klasa, progi hardcoded):
    - `LargePrRule` — additions+deletions >500 → warning, >1000 → critical
    - `QuickReviewRule` — czas created→merged vs rozmiar (>50 linii & <10 min → warning, >100 linii & <5 min → critical)
    - `WeekendWorkRule` — createdAt/mergedAt w sobotę/niedzielę → info
    - `NightWorkRule` — createdAt/mergedAt 22:00-06:00 → info
    - `NoReviewRule` — reviews.isEmpty() → warning
    - `SelfMergeRule` — autor = jedyny reviewer z "APPROVED" → warning

11. **`AggregateRules`** — reguły na poziomie raportu (po przejściu przez PR-y):
    - % weekendowych >30% → warning "Wysoki odsetek pracy weekendowej"

12. **`ActivityAnalysisService`** — implementuje `ActivityAnalysisUseCase`:
    - Wstrzykuje `MergeRequestProvider` (reużycie) + `ReviewProvider` (nowy port) + `List<ActivityRule>`
    - `getContributors()`: fetch all PRs → extract unique authors → count per author
    - `analyzeActivity()`: fetch PRs by author → for each PR fetch reviews → apply rules → aggregate stats → build heatmap data → build report

### Warstwa 4: Adapter out — GitHub reviews

13. **`GitHubReview` DTO** — `user.login`, `state` (APPROVED/CHANGES_REQUESTED/COMMENTED), `submitted_at`

14. **`GitHubClient` rozszerzenie** — nowa metoda `fetchReviews(owner, repo, prNumber)`:
    - `GET /repos/{owner}/{repo}/pulls/{number}/reviews`
    - Rate-limit handling (jak istniejące metody)

15. **`GitHubReviewAdapter`** — implementuje `ReviewProvider`, deleguje do `GitHubClient`, mapuje `GitHubReview` → `ReviewInfo`

### Warstwa 5: Adapter in — REST

16. **`ActivityController`** (`@RestController`, `@RequestMapping("/api/activity")`):
    - `GET /{owner}/{repo}/contributors` → `List<ContributorResponse>`
    - `GET /{owner}/{repo}/report?author={author}` → `ActivityReportResponse`

17. **`ContributorResponse`** — `(String login, int prCount)`

18. **`ActivityReportResponse`** — JSON:
    ```json
    {
      "contributor": "jan.kowalski",
      "projectSlug": "owner/repo",
      "stats": { "totalPrs": 15, "avgSize": 320, "avgReviewTimeMinutes": 45.5, "weekendPercentage": 13.3, "flagCounts": {"CRITICAL": 1, "WARNING": 3, "INFO": 2} },
      "flags": [ { "type": "VERY_LARGE_PR", "severity": "CRITICAL", "description": "...", "prReference": "#42" } ],
      "dailyActivity": { "2026-03-02": { "count": 2, "pullRequests": [...] }, ... },
      "pullRequests": [ { "id": "42", "title": "...", "size": 850, "createdAt": "...", "mergedAt": "...", "flags": [...] } ]
    }
    ```

### Warstwa 6: Frontend

19. **Typy TypeScript** (`types/activity.ts`) — `ActivityReport`, `ActivityFlag`, `ContributorStats`, `DailyActivity`, `Severity`

20. **API service** (`services/activityApi.ts`) — `getContributors(slug)`, `getActivityReport(slug, author)`

21. **`ContributorSelector`** — dropdown z listą kontrybutorów (login + PR count badge)

22. **`StatsCards`** — 4-5 kart Bootstrap: łączna liczba PR-ów, średni rozmiar, średni czas review, % weekendowe, flagi per severity

23. **`FlagsList`** — tabela flag z kolorowymi badge'ami severity, opisem, linkiem do PR

24. **`ActivityHeatmap`** — komponent SVG:
    - Grid: kolumny = tygodnie (ostatnie ~13 tygodni), wiersze = Pon-Niedz
    - Kwadraty 12×12px z 2px gap
    - Kolory: `#ebedf0` (0), `#9be9a8` (1), `#40c463` (2), `#30a14e` (3+), `#216e39` (5+)
    - Tooltip (CSS/title attr): "2026-03-15: 2 PR-y"
    - onClick → state `selectedDate` → render `DayDrillDown`
    - Etykiety: miesiące na górze, dni tygodnia po lewej (Pon, Śr, Pt)

25. **`DayDrillDown`** — lista PR-ów wybranego dnia (tytuł, rozmiar, flagi), renderowana pod heatmapą

26. **`ActivityDashboardPage`** — komponuje wszystko:
    - Header z nazwą repo
    - `ContributorSelector` → po wybraniu fetch report
    - `StatsCards` + `FlagsList` + `ActivityHeatmap` + `DayDrillDown`
    - Loading state, error state, empty state

27. **Route** w `App.tsx` — `/activity/:owner/:repo`

28. **Nawigacja** — link "Aktywność" w głównym menu/navbar (obok istniejącej nawigacji)

### Warstwa 7: BDD + testy

29. **`activity-analysis.feature`** — scenariusze Gherkin z spec.md (dashboard, flagi, brak aktywności, heatmapa)

30. **`ActivityAnalysisSteps.java`** — step definitions (mock `MergeRequestProvider` + `ReviewProvider`)

31. **Unit testy reguł** — każda reguła osobny test class: pozytywne, negatywne, edge case (np. PR dokładnie na progu)

32. **`ActivityAnalysisServiceTest`** — orkiestracja: mock providers → verify report structure

33. **`GitHubReviewAdapterTest`** — mapping DTO → ReviewInfo

34. **Frontend testy** (Vitest) — `ActivityHeatmap`, `FlagsList`, `StatsCards` — renderowanie z mock data

## Kolejność implementacji

```
Phase 1 — Domain core (no Spring, no UI):
  1. Enums: Severity, FlagType                    (krok 1-2)
  2. Records: ActivityFlag, ContributorStats,
     DailyActivity, ActivityReport                (krok 3-6)
  3. Ports: ActivityAnalysisUseCase, ReviewProvider (krok 7-8)
  4. Rules: interface + 6 implementacji            (krok 9-10)
  5. AggregateRules                                (krok 11)
  6. ActivityAnalysisService                       (krok 12)
  7. Unit testy reguł + service                    (krok 31-32)

Phase 2 — GitHub adapter:
  8. GitHubReview DTO                              (krok 13)
  9. GitHubClient.fetchReviews()                   (krok 14)
  10. GitHubReviewAdapter                          (krok 15)
  11. Test adaptera                                (krok 33)

Phase 3 — REST API:
  12. DTOs: ContributorResponse, ActivityReportResponse (krok 17-18)
  13. ActivityController                           (krok 16)
  14. BDD feature file + step defs                 (krok 29-30)

Phase 4 — Frontend:
  15. TypeScript typy + API service                (krok 19-20)
  16. ContributorSelector                          (krok 21)
  17. StatsCards                                   (krok 22)
  18. FlagsList                                    (krok 23)
  19. ActivityHeatmap + DayDrillDown               (krok 24-25)
  20. ActivityDashboardPage + route + nav           (krok 26-28)
  21. Frontend testy                               (krok 34)
```

## Ryzyka

| Ryzyko | Impact | Mitigation |
|--------|--------|------------|
| GitHub API rate limiting przy fetch reviews per PR | Wysoki — N+1 calls | Fetch reviews lazy (tylko merged PRs), limit analiza do ostatnich 100 PR-ów |
| Brak merge info w GitHub API (kto zmergował) | Średni — SelfMergeRule ograniczona | GitHub PR endpoint ma `merged_by` field — dodać do DTO/mapping |
| Duże repo → wolne ładowanie dashboardu | Średni | Limit 200 PR-ów, reviews fetch parallel (CompletableFuture) |
| Timezone edge case | Niski | Dokumentujemy: UTC only w P1, timezone jako przyszłe rozszerzenie |
