# 020: Plan implementacji — Cache aktywności + metryki wydajności kontrybutora

**Branch**: `020-activity-cache-velocity` | **Date**: 2026-03-25 | **Spec**: `specs/020-activity-cache-velocity/spec.md`

## Summary

Trójczęściowy feature: (1) incremental cache danych PR/reviews per-repo w module aktywności — initial full fetch + incremental update po `updatedAfter`, (2) metryki wydajności kontrybutora (velocity, cycle time, development impact, code churn, review engagement), (3) rozszerzenie domain modelu o `updatedAt` z abstrakcją provider-agnostic (GitHub + GitLab).

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.x (backend), React 18 + TypeScript (frontend)
**Primary Dependencies**: Spring Web, WebFlux client (GitHub/GitLab API), React-Bootstrap
**Storage**: In-memory cache (ConcurrentHashMap) z incremental update — nie full-refetch
**Testing**: JUnit 5 + Mockito (unit), Cucumber 7 (BDD), Vitest + RTL (frontend)
**Performance Goals**: Initial fetch: proporcjonalny do repo size. Update: <2s (tylko zmienione PRs). Cache hit: <100ms
**Constraints**: Domain model provider-agnostic; różnice GitHub/GitLab ukryte w adapterach; limit usunięty (docelowo tysiące PR-ów)

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | OK | Cache w domain service, incremental fetch w portach/adapterach |
| II. Provider Abstraction | OK | `MergeRequestProvider.fetchUpdatedSince()` — adaptery tłumaczą na `since` (GitHub) / `updated_after` (GitLab) |
| III. BDD Testing | OK | Feature files PRZED implementacją |
| IV. SDD Workflow | OK | spec.md → plan.md → tasks.md → implement |
| V. Simplicity (YAGNI) | OK | Cache in-memory, incremental = nowa metoda na porcie, nie nowy framework |

## Analiza API providerów — incremental fetch

### GitHub REST API

```
GET /repos/{owner}/{repo}/pulls?state=all&sort=updated&direction=desc&since=2026-03-20T00:00:00Z&per_page=100
```

- `sort=updated` — sortuj po `updated_at` (desc = newest first)
- `since` — zwróć tylko PRs zaktualizowane po danym timestamp (ISO 8601)
- `updated_at` zmienia się przy: nowym komentarzu, review, push, merge, label change, etc.
- Odpowiedź zawiera `updated_at` per PR
- Paginacja: Link header (jak dotychczas)

### GitLab REST API

```
GET /projects/{id}/merge_requests?state=all&order_by=updated_at&sort=desc&updated_after=2026-03-20T00:00:00Z&per_page=100
```

- `order_by=updated_at` — sortuj po updated
- `updated_after` — odpowiednik GitHub `since`
- `updated_at` per MR w odpowiedzi
- Paginacja: `page` + `per_page` (lub keyset pagination z `id_after`)

### Wspólny abstrakcyjny model

Oba API wspierają tę samą semantykę — różnią się tylko nazwy parametrów. Domain port:

```java
// Provider-agnostic:
List<MergeRequest> fetchMergeRequestsUpdatedSince(FetchCriteria criteria, LocalDateTime updatedAfter);
```

Adapter GitHub → `since=<ISO>` + `sort=updated`
Adapter GitLab → `updated_after=<ISO>` + `order_by=updated_at`

## Analiza istniejącego kodu — co się zmienia

### Domain model: `MergeRequest`

**Brakujące pole**: `updatedAt` (LocalDateTime). Oba API (GitHub, GitLab) zwracają to pole. Potrzebne do:
- Określenia "od kiedy" robić incremental fetch
- Sortowania wyników cache
- Poprawnego TTL (cache wie kiedy dane były ostatnio świeże)

**Zmiana**: Dodać `updatedAt` do `MergeRequest` + Builder. Backward-compatible — nullable, istniejący kod nie musi go używać.

### Domain model: `FetchCriteria`

Obecne pola: `projectSlug, targetBranch, state, after, before, limit`.

**Bez zmian** — `updatedAfter` NIE trafia do FetchCriteria. To parametr nowej metody na porcie, nie ogólne kryterium wyszukiwania. Czystsze API.

### Port: `MergeRequestProvider`

Obecne metody:
- `fetchMergeRequests(FetchCriteria)` — full list
- `fetchMergeRequest(projectSlug, mrId)` — single detail
- `getProviderName()` — "github" / "gitlab"

**Nowa metoda**:
```java
List<MergeRequest> fetchMergeRequestsUpdatedSince(String projectSlug, LocalDateTime updatedAfter);
```

Zwraca PRs (state=all) zaktualizowane po `updatedAfter`, posortowane po `updated_at` desc. Bez limitu (iteruj strony aż skończą się). Adaptery tłumaczą na API-specific params.

### Adapter: `GitHubClient` + `GitHubAdapter`

**GitHubClient**: Nowa metoda `fetchPullRequestsUpdatedSince(owner, repo, since)`:
- `state=all&sort=updated&direction=desc&since={since}`
- Paginacja: iteruj strony jak dotychczas (Link header)
- Zwraca shallow PRs (jak lista)

**GitHubAdapter**: Implementuje `fetchMergeRequestsUpdatedSince()` → deleguje do client, mapuje DTO → domain.

### `ActivityAnalysisService` — cache z incremental update

Obecny flow: każdy request = full GitHub fetch (1 + N + M calls).

**Nowy flow**:
```
1. getOrFetchCache(projectSlug):
   a) Cache MISS (first request):
      - Full fetch: fetchMergeRequests(all) → N detail calls → M review calls
      - Zapisz w cache z `lastUpdated = max(updatedAt)`
   b) Cache HIT + nie expired:
      - Return cache (0 API calls)
   c) Cache HIT + expired (TTL):
      - Incremental: fetchMergeRequestsUpdatedSince(projectSlug, lastUpdated)
      - Dla każdego zwróconego PR: re-fetch detail + reviews
      - Merge z cache (update istniejących, dodaj nowe)
      - Update `lastUpdated`
```

**Kluczowe**: Incremental update fetchuje TYLKO zmienione/nowe PRs, nie CAŁY repo. Dla repo z 3000 PRs gdzie w 15 min zmieni się 5 → fetch 5 zamiast 3000.

## Project Structure — nowe i zmienione pliki

```text
backend/src/main/java/com/mranalizer/
  domain/
    model/
      MergeRequest.java               # MODIFIED — dodane pole updatedAt (LocalDateTime, nullable)
    model/activity/
      ActivityRepoCache.java          # NEW — mutable cache: browsePrs, detailedPrs, reviewsByPr, lastUpdated
      VelocityMetrics.java            # NEW — record: prsPerWeek, weeklyBreakdown, trend
      CycleTimeMetrics.java           # NEW — record: avgHours, medianHours, p90Hours
      ImpactMetrics.java              # NEW — record: totalAdditions, totalDeletions, avgSize, addDeleteRatio
      CodeChurnMetrics.java           # NEW — record: churnRatio, label
      ReviewEngagementMetrics.java    # NEW — record: given, received, ratio, label
      ProductivityMetrics.java        # NEW — record agregujący wszystkie metryki
      ContributorStats.java           # MODIFIED — dodane pole: ProductivityMetrics productivity
    port/
      out/
        MergeRequestProvider.java     # MODIFIED — nowa metoda: fetchMergeRequestsUpdatedSince()
      in/activity/
        ActivityAnalysisUseCase.java  # MODIFIED — nowe metody: invalidateCache(), refreshCache()
    service/activity/
      ActivityAnalysisService.java    # MODIFIED — cache logic (initial + incremental) + metrics
      MetricsCalculator.java          # NEW — pure domain: oblicza metryki z List<MergeRequest> + reviews

  adapter/
    out/provider/github/
      GitHubAdapter.java              # MODIFIED — impl fetchMergeRequestsUpdatedSince()
      GitHubClient.java               # MODIFIED — nowa metoda fetchPullRequestsUpdatedSince()
      GitHubMapper.java               # MODIFIED — mapping updatedAt
      dto/
        GitHubPullRequest.java        # MODIFIED — dodane pole updated_at
    in/rest/activity/
      ActivityController.java         # MODIFIED — nowy endpoint POST /refresh
      dto/
        ActivityReportResponse.java   # MODIFIED — dodane pole: productivity

frontend/src/
  components/activity/
    ProductivityMetrics.tsx           # NEW — karty metryk wydajności
    VelocityChart.tsx                 # NEW — mini sparkline velocity per tydzień (SVG)
  types/
    activity.ts                      # MODIFIED — nowe typy metryk
  pages/
    ActivityDashboardPage.tsx         # MODIFIED — sekcja "Wydajność" + przycisk "Odśwież dane"

backend/src/test/
  java/com/mranalizer/
    domain/service/activity/
      MetricsCalculatorTest.java              # NEW — unit testy metryk
      ActivityAnalysisServiceCacheTest.java   # NEW — testy cache: hit/miss/incremental/TTL
    bdd/steps/
      ActivityCacheSteps.java                 # NEW — BDD steps cache
      ActivityMetricsSteps.java               # NEW — BDD steps metryk
  resources/features/
    activity-cache.feature                    # NEW — scenariusze cache + incremental
    activity-metrics.feature                  # NEW — scenariusze metryk
```

## Warstwy zmian (od wewnątrz hexagonu na zewnątrz)

### Warstwa 1: Domain model — MergeRequest + updatedAt

1. **`MergeRequest.java`** — dodać pole:
   ```java
   private LocalDateTime updatedAt;  // nullable, backward-compatible

   // + getter, + builder field
   ```

   **Backward-compatible**: istniejący kod nie używa `updatedAt`, nowe pole nullable. Testy nie wymagają zmian.

### Warstwa 2: Domain models — cache + metryki (pure Java, 0 deps)

2. **`ActivityRepoCache`** — **mutable** cache container (w odróżnieniu od immutable domain objects):
   ```java
   public class ActivityRepoCache {
       private final String projectSlug;
       private final Map<String, MergeRequest> detailedPrs;     // externalId → detail (mutable)
       private final Map<String, List<ReviewInfo>> reviewsByPr;  // externalId → reviews (mutable)
       private volatile LocalDateTime lastUpdated;               // max updatedAt z PRs — dla incremental
       private volatile Instant lastFetchedAt;                   // wall clock — dla TTL

       private static final Duration INCREMENTAL_TTL = Duration.ofMinutes(15);

       public boolean needsRefresh() {
           return Instant.now().isAfter(lastFetchedAt.plus(INCREMENTAL_TTL));
       }

       public LocalDateTime getLastUpdated() { return lastUpdated; }

       /** Merge nowe/zaktualizowane PRs do cache */
       public void mergeUpdatedPrs(List<MergeRequest> updatedPrs,
                                    Map<String, MergeRequest> updatedDetails,
                                    Map<String, List<ReviewInfo>> updatedReviews) {
           updatedDetails.forEach(detailedPrs::put);       // overwrite existing
           updatedReviews.forEach(reviewsByPr::put);       // overwrite existing
           lastUpdated = computeMaxUpdatedAt();
           lastFetchedAt = Instant.now();
       }

       public List<MergeRequest> getPrsForAuthor(String author) {
           return detailedPrs.values().stream()
               .filter(mr -> author.equals(mr.getAuthor()))
               .toList();
       }

       public List<MergeRequest> getAllDetailedPrs() {
           return List.copyOf(detailedPrs.values());
       }

       public Map<String, List<ReviewInfo>> getReviewsByPr() {
           return Collections.unmodifiableMap(reviewsByPr);
       }

       private LocalDateTime computeMaxUpdatedAt() {
           return detailedPrs.values().stream()
               .map(MergeRequest::getUpdatedAt)
               .filter(Objects::nonNull)
               .max(LocalDateTime::compareTo)
               .orElse(lastUpdated);
       }
   }
   ```

3. **Metryki — records** (pure domain, zero deps):

   **`VelocityMetrics`**:
   ```java
   public record VelocityMetrics(
       double prsPerWeek,
       List<WeeklyCount> weeklyBreakdown,
       String trend    // "rising" | "falling" | "stable"
   ) {
       public record WeeklyCount(LocalDate weekStart, int count) {}
   }
   ```

   **`CycleTimeMetrics`**:
   ```java
   public record CycleTimeMetrics(
       double avgHours,
       double medianHours,
       double p90Hours
   ) {}
   ```

   **`ImpactMetrics`**:
   ```java
   public record ImpactMetrics(
       int totalAdditions,
       int totalDeletions,
       int totalLines,
       double avgLinesPerPr,
       double addDeleteRatio
   ) {}
   ```

   **`CodeChurnMetrics`**:
   ```java
   public record CodeChurnMetrics(
       double churnRatio,
       String label
   ) {}
   ```

   **`ReviewEngagementMetrics`**:
   ```java
   public record ReviewEngagementMetrics(
       int reviewsGiven,
       int reviewsReceived,
       double ratio,
       String label
   ) {}
   ```

   **`ProductivityMetrics`** — agregat:
   ```java
   public record ProductivityMetrics(
       VelocityMetrics velocity,
       CycleTimeMetrics cycleTime,
       ImpactMetrics impact,
       CodeChurnMetrics codeChurn,
       ReviewEngagementMetrics reviewEngagement  // nullable — P3
   ) {}
   ```

4. **`ContributorStats` — rozszerzenie** — dodane pole `ProductivityMetrics productivity`.

### Warstwa 3: Domain port — incremental fetch

5. **`MergeRequestProvider`** — nowa metoda:
   ```java
   /**
    * Fetch MRs/PRs updated after given timestamp. Provider-agnostic:
    * GitHub uses "since" param, GitLab uses "updated_after" param.
    * Returns PRs sorted by updatedAt desc, state=all, no limit (paginate all).
    */
   List<MergeRequest> fetchMergeRequestsUpdatedSince(String projectSlug, LocalDateTime updatedAfter);
   ```

   **Dlaczego na porcie a nie w FetchCriteria**: `updatedAfter` to mechanizm synchronizacji cache, nie kryterium biznesowe. FetchCriteria to "co chce user" (branch, state, dates). `updatedAfter` to "co potrzebuje system" (incremental sync). Oddzielenie = czystsze API.

6. **`ActivityAnalysisUseCase`** — nowe metody:
   ```java
   void invalidateCache(String projectSlug);   // wymuś full refetch
   void refreshCache(String projectSlug);       // incremental update (force, bez czekania na TTL)
   ```

### Warstwa 4: Domain service — MetricsCalculator

7. **`MetricsCalculator`** — stateless, pure Java:
   ```java
   public class MetricsCalculator {
       private static final int VELOCITY_WEEKS = 4;

       public VelocityMetrics calculateVelocity(List<MergeRequest> mergedPrs) { ... }
       public CycleTimeMetrics calculateCycleTime(List<MergeRequest> mergedPrs) { ... }
       public ImpactMetrics calculateImpact(List<MergeRequest> prs) { ... }
       public CodeChurnMetrics calculateChurn(List<MergeRequest> prs) { ... }
       public ReviewEngagementMetrics calculateReviewEngagement(
           String author, List<MergeRequest> allRepoPrs,
           Map<String, List<ReviewInfo>> allReviews) { ... }

       public ProductivityMetrics calculateAll(
           String author, List<MergeRequest> userPrs,
           List<MergeRequest> allRepoPrs,
           Map<String, List<ReviewInfo>> allReviews) { ... }
   }
   ```

   **Algorytmy**:

   - **Velocity**: Filtruj merged PRs z ostatnich 4 tygodni → grupuj per tydzień (`mergedAt.toLocalDate()` week start = Monday) → avg = total / 4. Trend: last week count vs average → >1.2x = "rising", <0.8x = "falling", else "stable".

   - **Cycle time**: Filtruj PRs z `mergedAt != null` → `Duration.between(createdAt, mergedAt).toHours()` → sort → avg = mean, median = middle, p90 = element at `(int)(0.9 * size)`.

   - **Impact**: Sum `additions + deletions` per PR → totals, avg, ratio `additions / max(deletions, 1)`.

   - **Code churn**: `deletions / max(additions, 1)`. Label: <0.2 = "Głównie nowy kod", 0.2-0.8 = "Zbalansowany", >0.8 = "Przewaga refaktoringu".

   - **Review engagement**: Scan ALL reviews. `given` = reviewer == author na CUDZYCH PR-ach. `received` = reviews na PR-ach autora (reviewer != author). Ratio = given / max(received, 1). Label: >1.5 = "Aktywny reviewer", 0.5-1.5 = "Zbalansowany", <0.5 = "Mało review".

### Warstwa 5: Domain service — Cache z incremental update

8. **`ActivityAnalysisService` — refaktor**:

   ```java
   private final ConcurrentHashMap<String, ActivityRepoCache> repoCache = new ConcurrentHashMap<>();
   private final MetricsCalculator metricsCalculator;

   public ActivityReport analyzeActivity(String projectSlug, String author) {
       ActivityRepoCache cache = getOrFetchCache(projectSlug);
       List<MergeRequest> userPrs = cache.getPrsForAuthor(author);
       if (userPrs.isEmpty()) return emptyReport(author, projectSlug);

       Map<String, List<ReviewInfo>> reviewsByPr = cache.getReviewsByPr();
       List<ActivityFlag> allFlags = evaluateRules(userPrs, reviewsByPr);

       ProductivityMetrics productivity = metricsCalculator.calculateAll(
           author, userPrs, cache.getAllDetailedPrs(), reviewsByPr);

       ContributorStats stats = buildStats(userPrs, allFlags, productivity);
       Map<LocalDate, DailyActivity> dailyActivity = buildDailyActivity(userPrs, ...);
       return new ActivityReport(author, projectSlug, stats, allFlags, dailyActivity, userPrs);
   }

   private ActivityRepoCache getOrFetchCache(String projectSlug) {
       ActivityRepoCache cached = repoCache.get(projectSlug);

       if (cached == null) {
           // COLD START: full fetch
           log.info("Activity cache MISS for {} — full fetch", projectSlug);
           ActivityRepoCache fresh = fullFetch(projectSlug);
           repoCache.put(projectSlug, fresh);
           return fresh;
       }

       if (cached.needsRefresh()) {
           // INCREMENTAL UPDATE: fetch only changed PRs
           log.info("Activity cache STALE for {} — incremental update since {}",
               projectSlug, cached.getLastUpdated());
           incrementalUpdate(projectSlug, cached);
       }

       return cached;
   }

   /** Full fetch — initial load. Fetches ALL PRs, details, reviews. */
   private ActivityRepoCache fullFetch(String projectSlug) {
       FetchCriteria criteria = FetchCriteria.builder()
           .projectSlug(projectSlug).state("all").build();  // no limit — fetch all

       List<MergeRequest> browsePrs = mergeRequestProvider.fetchMergeRequests(criteria);
       Map<String, MergeRequest> details = fetchAllDetailsParallel(projectSlug, browsePrs);
       Map<String, List<ReviewInfo>> reviews = fetchAllReviewsParallel(projectSlug, details.values());

       return new ActivityRepoCache(projectSlug, details, reviews, Instant.now());
   }

   /** Incremental update — fetch only PRs updated since last sync. */
   private void incrementalUpdate(String projectSlug, ActivityRepoCache cache) {
       LocalDateTime since = cache.getLastUpdated();
       List<MergeRequest> updatedBrowse =
           mergeRequestProvider.fetchMergeRequestsUpdatedSince(projectSlug, since);

       if (updatedBrowse.isEmpty()) {
           cache.touchLastFetched();  // no changes, just update TTL
           log.info("Incremental update for {}: 0 changed PRs", projectSlug);
           return;
       }

       log.info("Incremental update for {}: {} changed PRs", projectSlug, updatedBrowse.size());

       // Re-fetch details + reviews only for changed PRs
       Map<String, MergeRequest> updatedDetails = fetchAllDetailsParallel(projectSlug, updatedBrowse);
       Map<String, List<ReviewInfo>> updatedReviews = fetchAllReviewsParallel(projectSlug, updatedDetails.values());

       cache.mergeUpdatedPrs(updatedBrowse, updatedDetails, updatedReviews);
   }

   public void invalidateCache(String projectSlug) {
       repoCache.remove(projectSlug);
       log.info("Activity cache INVALIDATED for {}", projectSlug);
   }

   public void refreshCache(String projectSlug) {
       ActivityRepoCache cached = repoCache.get(projectSlug);
       if (cached != null) {
           incrementalUpdate(projectSlug, cached);
       }
       // if no cache, next analyzeActivity() will do full fetch
   }
   ```

   **FetchCriteria.limit usunięty** (dla activity): Docelowo tysiące PR-ów. Limit 200 obcinał dane. Zamiast limitu — paginacja fetchuje WSZYSTKIE PRs. Przy incremental update to OK — cold start jest jednorazowy.

9. **`getContributors()` — z cache**:
   ```java
   public List<ContributorInfo> getContributors(String projectSlug) {
       ActivityRepoCache cache = getOrFetchCache(projectSlug);
       return cache.getAllDetailedPrs().stream()
           .filter(mr -> mr.getAuthor() != null)
           .collect(groupingBy(MergeRequest::getAuthor, counting()))
           .entrySet().stream()
           .map(e -> new ContributorInfo(e.getKey(), e.getValue().intValue()))
           .sorted(comparingInt(ContributorInfo::prCount).reversed())
           .toList();
   }
   ```

### Warstwa 6: Adapter out — GitHub incremental

10. **`GitHubPullRequest` DTO** — dodane pole:
    ```java
    @JsonProperty("updated_at") private String updatedAt;
    ```

11. **`GitHubMapper`** — mapping `updatedAt` → `MergeRequest.updatedAt` (parse ISO 8601 → LocalDateTime).

12. **`GitHubClient`** — nowa metoda:
    ```java
    public List<GitHubPullRequest> fetchPullRequestsUpdatedSince(
            String owner, String repo, LocalDateTime since) {
        // GET /repos/{owner}/{repo}/pulls?state=all&sort=updated&direction=desc
        //     &since={since.format(ISO_INSTANT)}&per_page=100
        // Paginate all pages (no limit)
    }
    ```

13. **`GitHubAdapter`** — implementacja:
    ```java
    @Override
    public List<MergeRequest> fetchMergeRequestsUpdatedSince(
            String projectSlug, LocalDateTime updatedAfter) {
        String[] parts = projectSlug.split("/");
        List<GitHubPullRequest> dtos = client.fetchPullRequestsUpdatedSince(
            parts[0], parts[1], updatedAfter);
        return dtos.stream().map(mapper::toDomainShallow).toList();
    }
    ```

    **Uwaga do przyszłego adaptera GitLab**: Identyczna semantyka, inne nazwy parametrów:
    ```java
    // GitLabClient:
    // GET /projects/{id}/merge_requests?state=all&order_by=updated_at
    //     &updated_after={since}&per_page=100
    ```

### Warstwa 7: Adapter in — REST

14. **`ActivityController`** — nowe endpointy:
    ```java
    @PostMapping("/{owner}/{repo}/refresh")
    public ResponseEntity<Void> refreshCache(
            @PathVariable String owner, @PathVariable String repo) {
        String projectSlug = validateSlug(owner, repo);
        activityAnalysis.refreshCache(projectSlug);  // incremental
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{owner}/{repo}/cache")
    public ResponseEntity<Void> invalidateCache(
            @PathVariable String owner, @PathVariable String repo) {
        String projectSlug = validateSlug(owner, repo);
        activityAnalysis.invalidateCache(projectSlug);  // full reset
        return ResponseEntity.noContent().build();
    }
    ```

    Dwa endpointy:
    - `POST /refresh` → incremental update (szybkie, domyślny przycisk)
    - `DELETE /cache` → full invalidation (wymusza cold start)

15. **`ActivityReportResponse`** — dodane pole `productivity` (JSON z metrykami).

### Warstwa 8: Frontend

16. **Typy** (`activity.ts`) — interfaces dla metryk.

17. **`ProductivityMetrics.tsx`** — karty Bootstrap:
    - Velocity: "3.0 PR/tyg" + trend arrow (↑↓→)
    - Cycle time: "median: 3h | avg: 15.6h | p90: 48h"
    - Impact: "3300 linii (2500+ / 800−)" z colored stacked bar
    - Churn: "0.32" + label
    - ReviewEngagement: dane lub "Niedostępne" (P3)

18. **`VelocityChart.tsx`** — SVG sparkline (4 słupki per tydzień).

19. **`ActivityDashboardPage.tsx`**:
    - Sekcja "Wydajność" między StatsCards a FlagsList
    - Przycisk "Odśwież dane" → `POST /refresh` (incremental)
    - Loading state

### Warstwa 9: BDD + testy

20. **`activity-cache.feature`**:
    ```gherkin
    Feature: Cache danych aktywności z incremental update

      Scenario: Pierwszy request — full fetch i cache
        Given cache jest pusty
        When pobieram raport aktywności dla repo "owner/repo" i autora "alice"
        Then system wykonuje full fetch z GitHub API
        And dane są cachowane

      Scenario: Cache hit przy zmianie kontrybutora
        Given dane repo "owner/repo" są w cache
        When pobieram raport aktywności dla autora "bob"
        Then system nie wykonuje żadnych GitHub API calls
        And raport zawiera dane autora "bob"

      Scenario: Incremental update po TTL
        Given dane repo "owner/repo" są w cache sprzed 16 minut
        And w repo pojawiły się 2 nowe PR-y od ostatniego fetch
        When pobieram raport aktywności
        Then system fetchuje tylko 2 zmienione PR-y (incremental)
        And cache zawiera zaktualizowane dane

      Scenario: Incremental update — zero zmian
        Given dane repo "owner/repo" są w cache sprzed 16 minut
        And w repo nie było żadnych zmian od ostatniego fetch
        When pobieram raport aktywności
        Then system sprawdza zmiany (1 API call)
        And cache TTL jest odświeżony

      Scenario: Manualna invalidacja — full refetch
        Given dane repo "owner/repo" są w cache
        When wysyłam DELETE /api/activity/owner/repo/cache
        Then cache jest wyczyszczony
        And następny request wykonuje full fetch

      Scenario: Manualny refresh — incremental
        Given dane repo "owner/repo" są w cache
        When wysyłam POST /api/activity/owner/repo/refresh
        Then system wykonuje incremental update
    ```

21. **`activity-metrics.feature`**:
    ```gherkin
    Feature: Metryki wydajności kontrybutora

      Scenario: Velocity — PR-y per tydzień
        Given kontrybutor "alice" z 8 zmergowanymi PR-ami w ostatnich 4 tygodniach
        When pobieram raport aktywności
        Then velocity wynosi 2.0 PR/tydzień

      Scenario: Cycle time — mediana i p90
        Given kontrybutor "alice" z PR-ami o czasach merge 1h, 2h, 3h, 24h, 48h
        When pobieram raport aktywności
        Then cycle time median wynosi 3.0 godzin
        And cycle time p90 wynosi 48.0 godzin

      Scenario: Development impact
        Given kontrybutor "alice" z PR-ami o rozmiarach (100+/50-), (200+/100-), (500+/200-)
        When pobieram raport aktywności
        Then total impact wynosi 1150 linii
        And churn ratio wynosi 0.44

      Scenario: Velocity zero przy braku merged PRs
        Given kontrybutor "alice" bez zmergowanych PR-ów
        When pobieram raport aktywności
        Then velocity wynosi 0.0 PR/tydzień
        And cycle time wyświetla "brak danych"

      Scenario: Trend spadkowy
        Given kontrybutor "alice" z 6 PR-ami w pierwszym tygodniu i 0 w pozostałych
        When pobieram raport aktywności
        Then velocity trend to "falling"

      Scenario: Review engagement
        Given kontrybutor "alice" dał 8 review i otrzymał 12 review
        When pobieram raport aktywności
        Then review engagement ratio wynosi 0.67
        And review engagement label to "Zbalansowany"
    ```

22. **`MetricsCalculatorTest`** — unit testy:
    - `calculateVelocity()` — happy path, zero PRs, uneven distribution, trend detection
    - `calculateCycleTime()` — happy path, single PR, no merged PRs, outlier handling
    - `calculateImpact()` — happy path, zero PRs, large numbers
    - `calculateChurn()` — labels per range, zero additions edge case
    - `calculateReviewEngagement()` — happy path, no reviews, self-reviews excluded

23. **`ActivityAnalysisServiceCacheTest`** — unit testy:
    - Full fetch on cold start (mock verify provider calls)
    - Cache hit — provider NOT called
    - Incremental update after TTL (mock `fetchMergeRequestsUpdatedSince`)
    - Incremental with 0 changes — only TTL touched
    - `invalidateCache` → next call = full fetch
    - `refreshCache` → incremental even if not expired
    - Merge semantics: updated PR overwrites old, new PR added

24. **Frontend testy** (Vitest):
    - `ProductivityMetrics` — render z mock data, null reviewEngagement
    - `VelocityChart` — 4 słupki

## Kolejność implementacji

```
Phase 1 — BDD feature files (test-first):
  1. activity-cache.feature               (cache + incremental scenarios)
  2. activity-metrics.feature             (metryki scenarios)

Phase 2 — Domain model (pure Java):
  3. MergeRequest — dodanie updatedAt
  4. ActivityRepoCache                    (mutable cache container z merge)
  5. Metryki records (Velocity, CycleTime, Impact, Churn, ReviewEngagement, Productivity)
  6. ContributorStats — dodanie productivity

Phase 3 — Domain service:
  7. MetricsCalculator                    (pure logic, all algorithms)
  8. MetricsCalculatorTest                (unit testy)
  9. ActivityAnalysisService — cache      (getOrFetchCache, fullFetch, incrementalUpdate)
  10. ActivityAnalysisService — metrics   (integrate MetricsCalculator)
  11. ActivityAnalysisServiceCacheTest     (cache + incremental testy)

Phase 4 — Port + adapter:
  12. MergeRequestProvider — fetchMergeRequestsUpdatedSince()
  13. GitHubPullRequest DTO — updatedAt
  14. GitHubMapper — updatedAt mapping
  15. GitHubClient — fetchPullRequestsUpdatedSince()
  16. GitHubAdapter — impl nowej metody

Phase 5 — REST:
  17. ActivityAnalysisUseCase — invalidateCache(), refreshCache()
  18. ActivityController — POST /refresh, DELETE /cache
  19. ActivityReportResponse — productivity field
  20. BDD step defs + run green

Phase 6 — Frontend:
  21. TypeScript typy metryk
  22. ProductivityMetrics.tsx + VelocityChart.tsx
  23. ActivityDashboardPage — sekcja wydajność + refresh
  24. Frontend testy (Vitest)
```

## Ryzyka

| Ryzyko | Impact | Mitigation |
|--------|--------|------------|
| Cold start na repo z 3000 PRs = 3000 detail + ~1500 review calls | Wysoki — rate limit 5000/h | Parallel fetch (thread pool), progress indicator na frontend, cache persists in memory |
| GitHub `since` param nie działa na `/pulls` (działa na `/issues`) | Wysoki — incremental nie zadziała | **Fallback**: sortuj po `updated`, iteruj strony aż `updatedAt < since` → stop. Efekt identyczny, minimalnie więcej API calls |
| Memory: 3000 MergeRequest × ~5KB = ~15MB per repo | Niski | Akceptowalne; max ~10 repo w cache = 150MB |
| Concurrent access: dwa requesty jednocześnie triggerują full fetch | Średni | `ConcurrentHashMap.computeIfAbsent()` + synchronized na incrementalUpdate |
| `updatedAt` null na starych PRs | Niski | Fallback: użyj `createdAt` jeśli `updatedAt` null |

## Trade-offs

1. **Incremental vs full-refetch**: Incremental — kluczowe dla repo z tysiącami PRs. Koszt: złożoność merge logic w cache. Warte tego.

2. **`updatedAfter` na porcie vs w FetchCriteria**: Na porcie jako osobna metoda — czyste oddzielenie "co chce user" od "co potrzebuje system (sync)".

3. **Limit usunięty**: Activity fetchuje WSZYSTKIE PRs (no limit). Cold start wolniejszy, ale dane kompletne. Incremental niweluje koszt przy kolejnych requestach.

4. **Mutable cache (vs immutable)**: `ActivityRepoCache` jest mutable (merge updates). Immutable wymagałby kopiowania 3000 elementów przy każdym update. Mutable + volatile/ConcurrentHashMap = bezpieczne i wydajne.

5. **Dwa endpointy (refresh vs invalidate)**: Refresh = incremental (szybki, default). Invalidate = full reset (gdy user podejrzewa stale data). Oba potrzebne.

6. **GitHub `since` fallback**: Jeśli `since` param nie działa na `/pulls`, fallback na sort+iterate. Adapter ukrywa tę różnicę — port abstraction działa.
