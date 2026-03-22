# 015: Tasks — Aktywność użytkownika + wykrywanie nieprawidłowości

**Input**: `specs/015-user-activity-health/spec.md`, `specs/015-user-activity-health/plan.md`

## Phase 1: Domain Core (pure Java, 0 deps)

**Purpose**: Modele, porty, reguły, service — fundament bez Springa

### BDD (test-first)

- [ ] T01 [US1] Feature file `activity-analysis.feature` w `backend/src/test/resources/features/` — wszystkie scenariusze Gherkin ze spec.md

### Modele domenowe

- [ ] T02 [P] Enum `Severity` w `domain/model/activity/Severity.java` — CRITICAL, WARNING, INFO
- [ ] T03 [P] Enum `FlagType` w `domain/model/activity/FlagType.java` — LARGE_PR, VERY_LARGE_PR, QUICK_REVIEW, SUSPICIOUS_QUICK_MERGE, WEEKEND_WORK, NIGHT_WORK, NO_REVIEW, SELF_MERGE, HIGH_WEEKEND_RATIO
- [ ] T04 [P] Record `ActivityFlag` w `domain/model/activity/ActivityFlag.java` — (FlagType, Severity, String description, String prReference)
- [ ] T05 [P] Record `ContributorStats` w `domain/model/activity/ContributorStats.java` — (totalPrs, avgSize, avgReviewTimeMinutes, weekendPercentage, flagCountBySeverity)
- [ ] T06 [P] Record `DailyActivity` w `domain/model/activity/DailyActivity.java` — (date, count, List<PrSummary>) + nested PrSummary record
- [ ] T07 `ActivityReport` w `domain/model/activity/ActivityReport.java` — contributor, projectSlug, stats, flags, dailyActivity, pullRequests

### Porty

- [ ] T08 [P] Port in `ActivityAnalysisUseCase` w `domain/port/in/activity/ActivityAnalysisUseCase.java` — getContributors(), analyzeActivity()
- [ ] T09 [P] Port out `ReviewProvider` w `domain/port/out/activity/ReviewProvider.java` — fetchReviews() + ReviewInfo record

### Reguły (Strategy pattern)

- [ ] T10 Interface `ActivityRule` w `domain/service/activity/rules/ActivityRule.java`
- [ ] T11 [P] `LargePrRule` w `domain/service/activity/rules/LargePrRule.java` — >500 warning, >1000 critical
- [ ] T12 [P] `QuickReviewRule` w `domain/service/activity/rules/QuickReviewRule.java` — czas vs rozmiar
- [ ] T13 [P] `WeekendWorkRule` w `domain/service/activity/rules/WeekendWorkRule.java` — sobota/niedziela
- [ ] T14 [P] `NightWorkRule` w `domain/service/activity/rules/NightWorkRule.java` — 22:00-06:00
- [ ] T15 [P] `NoReviewRule` w `domain/service/activity/rules/NoReviewRule.java` — 0 reviews
- [ ] T16 [P] `SelfMergeRule` w `domain/service/activity/rules/SelfMergeRule.java` — autor = reviewer APPROVED
- [ ] T17 `AggregateRules` w `domain/service/activity/rules/AggregateRules.java` — % weekendowe >30% → warning

### Service

- [ ] T18 `ActivityAnalysisService` w `domain/service/activity/ActivityAnalysisService.java` — implementuje UseCase, orkiestracja: fetch → rules → stats → heatmap → report

### Unit testy Phase 1

- [ ] T19 [P] `LargePrRuleTest` — próg 500, 1000, poniżej, na granicy
- [ ] T20 [P] `QuickReviewRuleTest` — szybki merge dużego PR, szybki merge małego PR (ok), wolny merge
- [ ] T21 [P] `WeekendWorkRuleTest` — sobota, niedziela, poniedziałek (ok)
- [ ] T22 [P] `NightWorkRuleTest` — 23:00 (flag), 03:00 (flag), 08:00 (ok), 22:00 (flag), 06:00 (ok)
- [ ] T23 [P] `NoReviewRuleTest` — 0 reviews (flag), 1+ reviews (ok)
- [ ] T24 [P] `SelfMergeRuleTest` — self-approve (flag), external approve (ok)
- [ ] T25 [P] `AggregateRulesTest` — 40% weekend (flag), 20% weekend (ok)
- [ ] T26 `ActivityAnalysisServiceTest` — mock providers, verify report structure, stats calculation, dailyActivity grouping

**Checkpoint Phase 1**: Domain kompletna, wszystkie unit testy przechodzą. Reguły działają na mock danych.

---

## Phase 2: GitHub Adapter (reviews)

**Purpose**: Rozszerzenie GitHubClient o reviews endpoint + adapter ReviewProvider

- [ ] T27 [P] DTO `GitHubReview` w `adapter/out/provider/github/dto/GitHubReview.java` — user.login, state, submitted_at
- [ ] T28 Metoda `fetchReviews()` w `adapter/out/provider/github/GitHubClient.java` — GET /repos/{owner}/{repo}/pulls/{number}/reviews
- [ ] T29 `GitHubReviewAdapter` w `adapter/out/provider/github/GitHubReviewAdapter.java` — implementuje ReviewProvider, mapuje DTO → ReviewInfo
- [ ] T30 `GitHubReviewAdapterTest` — mapping, error handling

**Checkpoint Phase 2**: Reviews z GitHub API działają, adapter przetestowany.

---

## Phase 3: REST API

**Purpose**: Endpointy HTTP dla frontendu

- [ ] T31 [P] DTO `ContributorResponse` w `adapter/in/web/activity/dto/ContributorResponse.java`
- [ ] T32 [P] DTO `ActivityReportResponse` w `adapter/in/web/activity/dto/ActivityReportResponse.java` — stats, flags, dailyActivity, pullRequests
- [ ] T33 `ActivityController` w `adapter/in/web/activity/ActivityController.java` — GET /{owner}/{repo}/contributors, GET /{owner}/{repo}/report?author=
- [ ] T34 BDD step definitions `ActivityAnalysisSteps.java` w `bdd/steps/` — implementacja kroków z T01

**Checkpoint Phase 3**: API zwraca poprawne JSON-y, BDD scenariusze przechodzą.

---

## Phase 4: Frontend — Dashboard (US1 + US2 + US3)

**Purpose**: Podstawowy dashboard z flagami i statystykami (P1 stories)

- [ ] T35 [P] Typy TypeScript w `frontend/src/types/activity.ts` — ActivityReport, ActivityFlag, ContributorStats, DailyActivity, Severity
- [ ] T36 [P] API service w `frontend/src/services/activityApi.ts` — getContributors(), getActivityReport()
- [ ] T37 `ContributorSelector` w `frontend/src/components/activity/ContributorSelector.tsx` — dropdown z listą + PR count badge
- [ ] T38 [P] `StatsCards` w `frontend/src/components/activity/StatsCards.tsx` — karty Bootstrap: total PRs, avg size, avg review time, weekend %, flag counts
- [ ] T39 [P] `FlagsList` w `frontend/src/components/activity/FlagsList.tsx` — tabela z severity badge, opis, PR reference
- [ ] T40 `ActivityDashboardPage` w `frontend/src/pages/ActivityDashboardPage.tsx` — kompozycja: header, selector, stats, flags. Loading/error/empty state
- [ ] T41 Route `/activity/:owner/:repo` w `App.tsx` + link "Aktywność" w nawigacji

**Checkpoint Phase 4**: Dashboard działa z selektorem, statystykami i flagami. Brak heatmapy (Phase 5).

---

## Phase 5: Frontend — Heatmapa (US6)

**Purpose**: Wizualizacja aktywności w stylu GitHub contribution graph

- [ ] T42 `ActivityHeatmap` w `frontend/src/components/activity/ActivityHeatmap.tsx` — SVG grid:
  - Kolumny = tygodnie (ostatnie ~13 tyg), wiersze = Pon-Niedz
  - Kwadraty 12×12px, 2px gap
  - 5 poziomów koloru: #ebedf0 (0), #9be9a8 (1), #40c463 (2), #30a14e (3-4), #216e39 (5+)
  - Etykiety miesięcy na górze, dni tygodnia po lewej (Pon, Śr, Pt)
  - Tooltip na hover (data + count)
  - onClick → selectedDate state
- [ ] T43 `DayDrillDown` w `frontend/src/components/activity/DayDrillDown.tsx` — lista PR-ów wybranego dnia (tytuł, rozmiar, flagi)
- [ ] T44 Integracja heatmapy + drill-down w `ActivityDashboardPage` — dodanie pod sekcją flag

**Checkpoint Phase 5**: Heatmapa renderuje się poprawnie, hover tooltip działa, kliknięcie pokazuje PR-y dnia.

---

## Phase 6: Frontend — Remaining P2 stories (US4, US5, US7)

**Purpose**: Weekend/night flags, no-review/self-merge, stats — te już działają w backendzie, frontend je wyświetla automatycznie (FlagsList + StatsCards). Weryfikacja end-to-end.

- [ ] T45 Weryfikacja end-to-end: weekend/night flagi widoczne w FlagsList z odpowiednimi ikonami/kolorami
- [ ] T46 Weryfikacja end-to-end: no-review/self-merge flagi widoczne
- [ ] T47 Weryfikacja end-to-end: stats (weekendPercentage, avgReviewTime) poprawne w StatsCards

**Checkpoint Phase 6**: Wszystkie user stories działają end-to-end.

---

## Phase 7: Testy frontend + polish

**Purpose**: Vitest testy + finalizacja

- [ ] T48 [P] Test `ActivityHeatmap.test.tsx` — renderowanie z mock data, kolory, tooltip, onClick
- [ ] T49 [P] Test `FlagsList.test.tsx` — severity sorting, badge colors, empty state
- [ ] T50 [P] Test `StatsCards.test.tsx` — renderowanie wartości, empty state
- [ ] T51 Pełny przebieg testów: `cd backend && mvn test` + `cd frontend && npx vitest run`
- [ ] T52 Aktualizacja `CLAUDE.md` — nowe komendy, moduł activity, route

**Checkpoint Phase 7**: Wszystkie testy przechodzą, dokumentacja aktualna.

---

## Dependencies & Execution Order

```
Phase 1 (Domain)     → Phase 2 (GitHub adapter) → Phase 3 (REST API)
                                                  ↓
Phase 4 (Frontend basic) → Phase 5 (Heatmapa) → Phase 6 (E2E verify) → Phase 7 (Tests + polish)
```

- Phase 1 nie wymaga Springa — pure Java, testowalne od razu
- Phase 2 i 3 mogą częściowo iść równolegle (DTOs niezależne)
- Phase 4 wymaga działającego API (Phase 3)
- Phase 5 (heatmapa) wymaga Phase 4 (dashboard page exists)
- Wewnątrz faz: taski oznaczone [P] mogą iść równolegle

## Notes

- Każdy task = osobny commit lub logiczna grupa commitów
- Feature branch `015-user-activity-health` — NIE pushować do master
- BDD feature file (T01) pisany PRZED implementacją (test-first)
- Reguły (T11-T16) w pełni niezależne — mogą być implementowane równolegle
