# 008 — Code Review Fixes: Plan techniczny

## Faza 1: Krytyczne bugi i bezpieczenstwo (P0)

### 1.1 NPE safety w MergeRequest + Rules
- `MergeRequest.java`: constructor/builder defaultuje `labels` i `changedFiles` do `List.of()`, `diffStats` do `new DiffStats(0,0,0)`
- Nie wymaga zmian w regulach — null juz nie dojdzie do regul
- BDD: scenariusze na MR z brakujacymi polami

### 1.2 ClaudeCliAdapter stderr
- `redirectErrorStream(true)` w ProcessBuilder — najprostsze rozwiazanie, stderr trafi do stdout i bedzie konsumowane
- Alternatywnie: osobny watek na stderr — ale overengineering

### 1.3 H2 console
- `application.yml`: wyniesc `h2.console.enabled: true` do profilu `dev` (spring profile)
- Domyslny profil: `h2.console.enabled: false`

### 1.4 GitHubClient HTTP error handling + SSRF
- `.onStatus()` handlery na WebClient:
  - 401/403 → `ProviderAuthException`
  - 404 → `ProviderException("Not found")`
  - 429 → `ProviderRateLimitException`
- Zamienic `String.format` na `UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/pulls")` z path variables
- Zamienic `NoSuchElementException` na `ProviderException`

## Faza 2: Bugi i architektura (P1)

### 2.1 BrowseMrService cache fix
- Klucz cache: zaimplementowac `FetchCriteria.cacheKey()` zwracajacy stabilny string z pelnych kryteriow
- Zamienic `containsKey()+get()` na `computeIfAbsent()`
- Dodac `BrowseCacheUseCase` port (lub rozszerzyc `BrowseMrUseCase`) — `invalidateCache()`, `hasCachedResults(criteria)`
- `BrowseRestController` zalezy od portu

### 2.2 Walidacja inputu (backend)
- `RepoRestController.addRepo`: walidacja null/blank projectSlug → 400
- `GitHubAdapter.fetchMergeRequest`: try-catch Integer.parseInt → `InvalidRequestException`
- `FetchCriteria`: dodac `validate()` — max limit 200, after < before
- Wywolac `validate()` w `AnalysisRequestDto.toFetchCriteria()`

### 2.3 Frontend TypeScript types + error handling
- `types/index.ts`: zmiana typow na nullable gdzie API moze zwrocic null
- `ScoreBadge.tsx`: null-safe `score?.toFixed(2)`
- `AnalysisHistory.tsx`: precompute seenReports przed render
- `DashboardPage.tsx`: error state dla loadRepos/loadHistory/addRepo
- `MrDetailPage.tsx`, `AnalysisDetailPage.tsx`: AbortController w useEffect

## Faza 3: Czyszczenie (P2/P3)

### 3.1 Dead code removal
- Usunac: `AnalysisForm.tsx`, `ProviderConfig.java`
- Usunac z types/index.ts: `getSummary`, `AnalysisSummary`, `ErrorResponse`
- Usunac z analysisApi.ts: `getSummary` export
- Usunac Lombok z pom.xml (nie jest uzywane)
- Zaczac uzywac spring-boot-starter-validation: `@Valid` na DTO w controllerach, `@NotBlank` na AnalysisRequestDto.projectSlug

### 3.2 DRY frontend utils
- Stworzyc `frontend/src/utils/format.ts`: `formatDate()`
- Stworzyc `frontend/src/utils/verdict.ts`: `verdictClass()`, `verdictLabel()`
- Stworzyc `frontend/src/utils/error.ts`: `extractApiError()`
- Podmienic uzycia w komponentach

### 3.3 Drobne poprawki
- `ClaudeCliAdapter.java`: podwojny srednik
- `MergeRequest.java`: `isHasTests()` → `hasTests()` (uwaga: wymaga zmian w uzytkownikach gettera)
- `ScoringEngine.java`: spójny exclude threshold check
- `BoostRule.byDescriptionKeywords`: rename na `byKeywords` lub poprawic javadoc
- `MrDetailResponse.ScoreBreakdownEntry.weight`: usunac pole lub zachowac weight z RuleResult

## Wplyw na testy

### Nowe scenariusze BDD
- `scoring-null-safety.feature`: MR bez labels/diffStats/changedFiles
- `validation.feature`: bledne inputy → 400
- `github-errors.feature`: GitHub API 401/403/429 → domain exceptions

### Istniejace testy
- Zmiana `isHasTests()` na `hasTests()` wymaga aktualizacji `ScoringSteps.java` i `ScoringEngineTest.java`
- Zmiana typow TS wymaga aktualizacji testow frontendowych (usuniecie `as unknown as` castow)

## Ryzyko

- Zmiana `isHasTests()` → `hasTests()` to breaking change jesli cos zewnetrznego uzywa gettera. W ramach projektu: pelna kontrola.
- Zmiana cache key moze spowodowac cache miss na istniejacych cache — akceptowalne, cache jest in-memory i volatile.
- Usuwanie `ProviderConfig` — upewnic sie ze zaden inny bean go nie importuje.
