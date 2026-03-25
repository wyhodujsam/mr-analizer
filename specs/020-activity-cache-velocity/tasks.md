# 020: Tasks — Cache aktywności z incremental update + metryki wydajności

**Input**: `specs/020-activity-cache-velocity/spec.md`, `specs/020-activity-cache-velocity/plan.md`

## Phase 1: BDD Feature Files (test-first)

**Purpose**: Scenariusze Gherkin PRZED implementacją — definiują oczekiwane zachowanie cache i metryk.

- [ ] T01 [US1] Feature file `activity-cache.feature` w `backend/src/test/resources/features/` — 6 scenariuszy: cold start, cache hit, incremental update, incremental zero changes, manual invalidation, manual refresh
- [ ] T02 [US2-6] Feature file `activity-metrics.feature` w `backend/src/test/resources/features/` — 6 scenariuszy: velocity, cycle time, impact+churn, velocity zero, trend falling, review engagement

**Checkpoint Phase 1**: Feature files gotowe, testy RED (brak implementacji).

---

## Phase 2: Domain Model (pure Java, 0 deps)

**Purpose**: Rozszerzenie MergeRequest o updatedAt, cache container, rekordy metryk.

### MergeRequest — updatedAt

- [ ] T03 [P] Pole `updatedAt` (LocalDateTime, nullable) w `domain/model/MergeRequest.java` + getter + builder field. Backward-compatible — istniejące testy bez zmian.

### Cache container

- [ ] T04 [P] `ActivityRepoCache` w `domain/model/activity/ActivityRepoCache.java` — mutable cache:
  - Pola: `projectSlug`, `Map<String, MergeRequest> detailedPrs`, `Map<String, List<ReviewInfo>> reviewsByPr`, `volatile LocalDateTime lastUpdated`, `volatile Instant lastFetchedAt`
  - Metody: `needsRefresh()` (TTL 15 min), `mergeUpdatedPrs()`, `touchLastFetched()`, `getPrsForAuthor(author)`, `getAllDetailedPrs()`, `getReviewsByPr()`, `computeMaxUpdatedAt()`
  - TTL konfigurowalny (Duration w konstruktorze)

### Metryki — records

- [ ] T05 [P] `VelocityMetrics` record w `domain/model/activity/VelocityMetrics.java` — prsPerWeek, List<WeeklyCount> weeklyBreakdown, trend (String). Nested record `WeeklyCount(LocalDate weekStart, int count)`.
- [ ] T06 [P] `CycleTimeMetrics` record w `domain/model/activity/CycleTimeMetrics.java` — avgHours, medianHours, p90Hours
- [ ] T07 [P] `ImpactMetrics` record w `domain/model/activity/ImpactMetrics.java` — totalAdditions, totalDeletions, totalLines, avgLinesPerPr, addDeleteRatio
- [ ] T08 [P] `CodeChurnMetrics` record w `domain/model/activity/CodeChurnMetrics.java` — churnRatio, label (String)
- [ ] T09 [P] `ReviewEngagementMetrics` record w `domain/model/activity/ReviewEngagementMetrics.java` — reviewsGiven, reviewsReceived, ratio, label (String)
- [ ] T10 [P] `ProductivityMetrics` record w `domain/model/activity/ProductivityMetrics.java` — velocity, cycleTime, impact, codeChurn, reviewEngagement (nullable)

### ContributorStats — rozszerzenie

- [ ] T11 `ContributorStats` w `domain/model/activity/ContributorStats.java` — dodanie pola `ProductivityMetrics productivity`. Aktualizacja wszystkich miejsc tworzenia (ActivityAnalysisService.buildStats, testy).

**Checkpoint Phase 2**: Modele kompilują się, istniejące testy przechodzą.

---

## Phase 3: Domain Service — MetricsCalculator (pure logic)

**Purpose**: Stateless kalkulator metryk — pure Java, zero deps.

- [ ] T12 `MetricsCalculator` w `domain/service/activity/MetricsCalculator.java`:
  - `calculateVelocity(List<MergeRequest> mergedPrs)` — merged PRs z ostatnich 4 tygodni, grupuj per week (Monday start), avg = total/4, trend: last week vs avg (>1.2x rising, <0.8x falling, else stable)
  - `calculateCycleTime(List<MergeRequest> mergedPrs)` — Duration createdAt→mergedAt w godzinach, sort, avg/median/p90
  - `calculateImpact(List<MergeRequest> prs)` — sum additions+deletions, avg per PR, ratio add/del
  - `calculateChurn(List<MergeRequest> prs)` — deletions/max(additions,1), label: <0.2 "Głównie nowy kod", 0.2-0.8 "Zbalansowany", >0.8 "Przewaga refaktoringu"
  - `calculateReviewEngagement(author, allRepoPrs, allReviews)` — given = author jako reviewer cudzych PRs, received = reviews na PR-ach autora (reviewer != author), ratio, label: >1.5 "Aktywny reviewer", 0.5-1.5 "Zbalansowany", <0.5 "Mało review"
  - `calculateAll(author, userPrs, allRepoPrs, allReviews)` → ProductivityMetrics

### Unit testy MetricsCalculator

- [ ] T13 `MetricsCalculatorTest` w `domain/service/activity/MetricsCalculatorTest.java`:
  - **Velocity**: happy path (8 PRs / 4 tyg = 2.0), zero PRs (0.0), nierówna dystrybucja (6+0+0+0 = 1.5, trend falling), trend rising/stable
  - **Cycle time**: happy path (5 PRs z różnymi czasami → avg/median/p90), single PR (avg=median=p90), no merged PRs (all zeros)
  - **Impact**: happy path (sumy, avg, ratio), zero PRs (all zeros), edge: 0 deletions → ratio = additions/1
  - **Churn**: labels per range, zero additions → churn = 0 (nie dzielenie przez zero), zero deletions → 0.0
  - **Review engagement**: happy path, no reviews (0/0, ratio 0), self-reviews excluded, author-only reviews

**Checkpoint Phase 3**: MetricsCalculator przetestowany, green.

---

## Phase 4: Domain Service — Cache z incremental update

**Purpose**: Refaktor ActivityAnalysisService — cache logic + incremental + integracja metryk.

- [ ] T14 Refaktor `ActivityAnalysisService` — cache infrastructure:
  - Nowe pole: `ConcurrentHashMap<String, ActivityRepoCache> repoCache`
  - Nowe pole: `MetricsCalculator metricsCalculator` (wstrzyknięty)
  - Metoda `getOrFetchCache(projectSlug)`:
    - Cache null → `fullFetch()` → put
    - Cache exists + not expired → return
    - Cache exists + expired → `incrementalUpdate()` → return
  - Metoda `fullFetch(projectSlug)` — FetchCriteria (state=all, no limit) → fetchMergeRequests → fetchAllDetailsParallel (ALL PRs) → fetchAllReviewsParallel (ALL merged PRs) → new ActivityRepoCache
  - Metoda `incrementalUpdate(projectSlug, cache)` — `fetchMergeRequestsUpdatedSince(lastUpdated)` → if empty: touchLastFetched + return → else: fetchDetailsParallel(updated) → fetchReviewsParallel(updated) → cache.mergeUpdatedPrs()
  - `invalidateCache(projectSlug)` → repoCache.remove()
  - `refreshCache(projectSlug)` → incrementalUpdate (force, bez TTL check)

- [ ] T15 Refaktor `ActivityAnalysisService.analyzeActivity()` — z cache + metryki:
  - `getOrFetchCache()` zamiast bezpośrednich provider calls
  - `evaluateRules()` z danych cache
  - `metricsCalculator.calculateAll()` → ProductivityMetrics
  - `buildStats()` z productivity
  - Usunięcie starych metod: `fetchDetailsParallel(userPrsBrowse)` → `fetchAllDetailsParallel(allPrs)`

- [ ] T16 Refaktor `ActivityAnalysisService.getContributors()` — z cache (nie bezpośredni provider call)

### Unit testy cache

- [ ] T17 `ActivityAnalysisServiceCacheTest` w `domain/service/activity/ActivityAnalysisServiceCacheTest.java`:
  - **Cold start**: provider.fetchMergeRequests + fetchMergeRequest (N) + reviewProvider.fetchReviews (M) called
  - **Cache hit**: provider NIE called, raport zwrócony z cache
  - **Cache hit inny autor**: provider NIE called, raport zawiera dane filtrowane per author
  - **Incremental update po TTL**: provider.fetchMergeRequestsUpdatedSince called, fetchMergeRequest only for returned PRs
  - **Incremental zero zmian**: fetchMergeRequestsUpdatedSince returns empty → provider.fetchMergeRequest NIE called, TTL refreshed
  - **invalidateCache**: next call = full fetch
  - **refreshCache**: incremental even if not expired
  - **Merge semantics**: updated PR overwrites old in cache, new PR added
  - **getContributors z cache**: provider NIE called jeśli cache exists

**Checkpoint Phase 4**: Cache + incremental działa, metryki zintegrowane, unit testy green.

---

## Phase 5: Port + Adapter — incremental fetch

**Purpose**: Nowa metoda na porcie + implementacja GitHub adapter.

### Port

- [ ] T18 [P] Nowa metoda w `MergeRequestProvider`:
  ```java
  List<MergeRequest> fetchMergeRequestsUpdatedSince(String projectSlug, LocalDateTime updatedAfter);
  ```
  Semantyka: state=all, sort by updatedAt desc, paginate all (no limit). Adaptery tłumaczą na API-specific params.

### GitHub adapter

- [ ] T19 Pole `updated_at` w `GitHubPullRequest` DTO — `@JsonProperty("updated_at") String updatedAt`
- [ ] T20 Mapping `updatedAt` w `GitHubMapper` — parse ISO 8601 → LocalDateTime, set na MergeRequest.Builder. Również w istniejących metodach mapping (toDomainShallow, toDomainWithFiles).
- [ ] T21 Nowa metoda `GitHubClient.fetchPullRequestsUpdatedSince(owner, repo, since)`:
  - `GET /repos/{owner}/{repo}/pulls?state=all&sort=updated&direction=desc&per_page=100`
  - Paginacja: iteruj strony, stop gdy `updatedAt < since` (fallback jeśli `since` param nie filtruje server-side)
  - Rate limit handling jak istniejące metody
- [ ] T22 Implementacja `GitHubAdapter.fetchMergeRequestsUpdatedSince()` — deleguje do client, mapuje DTO → domain

### Port in — rozszerzenie

- [ ] T23 Nowe metody w `ActivityAnalysisUseCase`:
  ```java
  void invalidateCache(String projectSlug);
  void refreshCache(String projectSlug);
  ```

**Checkpoint Phase 5**: Incremental fetch działa z GitHub API, port abstrakcja gotowa na GitLab.

---

## Phase 6: REST API

**Purpose**: Endpointy HTTP dla cache management + metryki w response.

- [ ] T24 `ActivityController` — nowe endpointy:
  - `POST /{owner}/{repo}/refresh` → `activityAnalysis.refreshCache()` → 204 No Content
  - `DELETE /{owner}/{repo}/cache` → `activityAnalysis.invalidateCache()` → 204 No Content
  - Walidacja slug (istniejący pattern SLUG_PART)

- [ ] T25 `ActivityReportResponse` — dodanie pola `productivity` (ProductivityMetricsResponse):
  - Nested DTOs: VelocityResponse, CycleTimeResponse, ImpactResponse, ChurnResponse, ReviewEngagementResponse
  - Mapping z domain ProductivityMetrics → response DTO w `ActivityReportResponse.from()`
  - null-safe: reviewEngagement nullable

- [ ] T26 BDD step definitions:
  - `ActivityCacheSteps.java` — kroki z activity-cache.feature (mock provider, verify call counts, TTL manipulation)
  - `ActivityMetricsSteps.java` — kroki z activity-metrics.feature (mock data → verify metryki w response)
  - Run BDD → ALL GREEN

**Checkpoint Phase 6**: API kompletne, BDD green, metryki w JSON response.

---

## Phase 7: Frontend

**Purpose**: UI metryk wydajności + przycisk refresh.

### Typy

- [ ] T27 Rozszerzenie `types/activity.ts`:
  - `ProductivityMetrics` interface (velocity, cycleTime, impact, codeChurn, reviewEngagement?)
  - `VelocityMetrics` (prsPerWeek, weeklyBreakdown, trend)
  - `CycleTimeMetrics` (avgHours, medianHours, p90Hours)
  - `ImpactMetrics` (totalAdditions, totalDeletions, totalLines, avgLinesPerPr, addDeleteRatio)
  - `CodeChurnMetrics` (churnRatio, label)
  - `ReviewEngagementMetrics` (reviewsGiven, reviewsReceived, ratio, label)

### Komponenty

- [ ] T28 `ProductivityMetrics.tsx` w `components/activity/`:
  - 4-5 kart Bootstrap w Row/Col:
    - **Velocity**: "3.0 PR/tyg" + trend arrow (↑ green / ↓ red / → gray) + sparkline
    - **Cycle time**: "median: 3h" duży, "avg: 15.6h | p90: 48h" mniejszy
    - **Impact**: "3300 linii" + stacked bar (green additions / red deletions)
    - **Churn**: "0.32" + label badge ("Zbalansowany")
    - **Review engagement**: dane lub "Niedostępne" jeśli null
  - Props: `productivity: ProductivityMetrics | null`

- [ ] T29 `VelocityChart.tsx` w `components/activity/`:
  - SVG sparkline: 4 słupki (per tydzień), max height 40px, width auto
  - Kolor: primary blue, last week highlighted jeśli trend
  - Tooltip z liczbą PRs per tydzień

- [ ] T30 Zmiany w `ActivityDashboardPage.tsx`:
  - Sekcja "Wydajność" (`<h5>Wydajność</h5>`) między StatsCards a FlagsList
  - Render `<ProductivityMetrics>` z danych raportu
  - Przycisk "Odśwież dane" (Bootstrap outline-secondary, ikona refresh):
    - onClick: `POST /api/activity/{owner}/{repo}/refresh` → re-fetch report
    - Loading state (spinner na przycisku)
  - API call w `activityApi.ts`: `refreshActivityCache(owner, repo)`

### Frontend testy

- [ ] T31 Testy Vitest:
  - `ProductivityMetrics.test.tsx` — render z pełnymi danymi, render z null reviewEngagement, render z null productivity (brak sekcji)
  - `VelocityChart.test.tsx` — render 4 słupków, empty data

**Checkpoint Phase 7**: UI kompletne, testy green.

---

## Phase 8: Integration + Polish

**Purpose**: Pełny test E2E, CLAUDE.md update, cleanup.

- [ ] T32 Uruchomić WSZYSTKIE testy: `cd backend && mvn test` + `cd frontend && npx vitest run`
- [ ] T33 Update `CLAUDE.md` — sekcja Activity Module: dodać info o cache (incremental, TTL), metrykach, nowych endpointach
- [ ] T34 Update `SPEC.md` — sekcja 020, REST API (nowe endpointy), zrealizowane features

**Checkpoint Phase 8**: Wszystkie testy green, dokumentacja aktualna.

---

## Dependency Graph

```
T01, T02 (BDD files)
    ↓
T03 (MergeRequest.updatedAt) ─── niezależne od T04-T11
    ↓
T18 (Port: fetchUpdatedSince) → T19-T22 (GitHub adapter)
    ↓
T04 (Cache container) → T14-T16 (Service cache) → T17 (Cache tests)
    ↓
T05-T10 (Metrics records) → T11 (ContributorStats) → T12 (MetricsCalculator) → T13 (Calculator tests)
    ↓
T23 (Port in: invalidate/refresh) → T24 (Controller) → T25 (Response DTO) → T26 (BDD steps)
    ↓
T27-T31 (Frontend)
    ↓
T32-T34 (Integration + polish)
```

## Szacunek złożoności

| Phase | Tasks | Złożoność | Opis |
|-------|-------|-----------|------|
| 1 — BDD files | T01-T02 | S | Gherkin scenariusze |
| 2 — Domain models | T03-T11 | S | Records + 1 mutable class |
| 3 — MetricsCalculator | T12-T13 | M | 5 algorytmów + testy |
| 4 — Cache + incremental | T14-T17 | L | Główna złożoność: cache logic, incremental, merge, concurrency |
| 5 — Port + adapter | T18-T23 | M | Nowa metoda GitHub API + mapping |
| 6 — REST | T24-T26 | S | 2 endpointy + response DTO + BDD steps |
| 7 — Frontend | T27-T31 | M | Nowe komponenty + chart + refresh |
| 8 — Polish | T32-T34 | S | Testy + docs |
