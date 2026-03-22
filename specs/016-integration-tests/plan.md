# 016: Plan implementacji — Testy integracyjne E2E

**Branch**: `016-integration-tests` | **Date**: 2026-03-22 | **Spec**: `specs/016-integration-tests/spec.md`

## Summary

Testy integracyjne z MockWebServer symulującym GitHub API. Pełny Spring Boot context bez mocków providerów — weryfikacja łańcucha HTTP → DTO → domain → REST. Wyłapują bugi których unit testy z mockami nie widzą.

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.x
**Dependencies**: MockWebServer (już w pom.xml), Spring Boot Test, TestRestTemplate, H2
**Testing**: JUnit 5, AssertJ
**Nowe**: `@DynamicPropertySource` do wstrzykiwania URL MockWebServer

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal | OK | Testujemy adaptery + domain razem, bez mocków portów |
| III. BDD | OK | Testy BDD uzupełniamy o warstwę integracyjną (nie zastępujemy) |
| IV. SDD | OK | spec → plan → tasks → implement |
| V. YAGNI | OK | MockWebServer już jest, nie dodajemy nowych deps |

## Warstwy zmian

### Warstwa 1: Infrastruktura testowa

1. **`application-integration-github.yml`** — profil testowy:
   - `mr-analizer.github.api-url`: placeholder (nadpisany przez DynamicPropertySource)
   - `mr-analizer.github.token`: `test-token`
   - `mr-analizer.llm.adapter`: `none`
   - `spring.datasource.url`: `jdbc:h2:mem:integration`
   - `spring.jpa.hibernate.ddl-auto`: `create-drop`

2. **`GitHubIntegrationTestBase.java`** — klasa bazowa:
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`
   - `@ActiveProfiles("integration-github")`
   - Static `MockWebServer githubApi` — start w `@BeforeAll`, shutdown w `@AfterAll`
   - `@DynamicPropertySource` — `mr-analizer.github.api-url` → MockWebServer URL
   - Helper: `enqueueGitHubResponse(String fixturePath)` — ładuje JSON z resources
   - Helper: `enqueueError(int status, String body)`
   - **BEZ** `@MockBean` na MergeRequestProvider / ReviewProvider / LlmAnalyzer

3. **Problem**: `LlmAnalyzer` — potrzebuje bean, ale nie chcemy prawdziwego Claude CLI w testach integracyjnych.
   - Rozwiązanie: profil `integration-github` + `mr-analizer.llm.adapter: none` → Spring załaduje `NoOpLlmAdapter`

### Warstwa 2: GitHub API fixtures (realistyczne JSON-y)

4. **`src/test/resources/github-fixtures/`**:
   - `pr-list-3.json` — lista 3 PR-ów BEZ `additions/deletions/changed_files` (tak jak prawdziwe `GET /pulls`)
   - `pr-single-1.json` — single PR #1 Z `additions: 350, deletions: 50, changed_files: 8`
   - `pr-single-2.json` — single PR #2 Z `additions: 800, deletions: 300, changed_files: 15` (duży diff)
   - `pr-single-3.json` — single PR #3 Z plikami testowymi
   - `pr-files-1.json` — pliki PR #1 (normalne)
   - `pr-files-2.json` — pliki PR #2 (duże)
   - `pr-files-3.json` — pliki PR #3 (z testami)
   - `pr-reviews-empty.json` — `[]`
   - `pr-reviews-approved.json` — 1 review APPROVED od external reviewer
   - `pr-reviews-self.json` — 1 review APPROVED od autora (self-merge)
   - `error-401.json` — `{"message": "Bad credentials"}`
   - `error-429.json` — `{"message": "API rate limit exceeded"}`

   JSON-y wzorowane na prawdziwych odpowiedziach GitHub API v3 (pełne nested objects: `user.login`, `head.ref`, `base.ref`, `labels[].name`).

### Warstwa 3: Testy integracyjne

5. **`BrowseIntegrationTest`** (US1):
   - `browse_returnsZeroChangedFiles_whenGitHubListDoesNotIncludeThem()` — enqueue lista bez additions → verify response `changedFilesCount=0` → **dokumentuje znany limit**
   - `browse_returnsCorrectMetadata_fromGitHubListEndpoint()` — title, author, state, dates, labels

6. **`AnalysisIntegrationTest`** (US2):
   - `analysis_returnsCorrectScores_withRealisticGitHubData()` — enqueue lista + 3x single PR + 3x files → POST analysis → verify 3 results z score > 0
   - `analysis_appliesPenalty_forLargeDiff()` — PR z 1100 liniami → verify score penalty
   - `analysis_appliesBoost_forTestFiles()` — PR z test files → verify boost

7. **`ActivityIntegrationTest`** (US3):
   - `activityReport_hasNonZeroAvgSize_fromSinglePrEndpoint()` — enqueue lista (bez additions) + single PRs (z additions) → GET report → verify `avgSize > 0` — **wyłapuje bug!**
   - `activityReport_detectsNoReview()` — enqueue reviews empty → verify flag
   - `activityReport_detectsQuickMerge()` — PR merged in 2 min → verify flag
   - `contributors_returnsUniqueAuthors()` — enqueue lista z 3 autorami → verify count

8. **`GitHubErrorHandlingIntegrationTest`** (US4):
   - `browse_returns401_whenGitHubReturnsUnauthorized()` — enqueue 401 → verify REST response status
   - `browse_returns429_whenGitHubRateLimited()` — enqueue 429 → verify
   - `activity_handlesGitHubErrors_gracefully()` — enqueue 404 na reviews → verify report without crash

9. **`GitHubContractIntegrationTest`** (US5):
   - `fullPrJson_isParsedCorrectly()` — realistyczny JSON z wszystkimi polami → verify domain model
   - `reviewsJson_isParsedCorrectly()` — reviews z różnymi state → verify

### Warstwa 4: BDD scenarios (opcjonalnie)

10. **`integration-github.feature`** — scenariusze Gherkin dla pełnego flow z realistycznym API (jeśli chcemy BDD coverage; alternatywnie same JUnit testy wystarczą)

## Kolejność implementacji

```
1. Profil + base class (krok 1-3)
2. JSON fixtures (krok 4)
3. BrowseIntegrationTest (krok 5) — waliduje znany bug
4. AnalysisIntegrationTest (krok 6)
5. ActivityIntegrationTest (krok 7) — waliduje bug avgSize=0
6. ErrorHandlingIntegrationTest (krok 8)
7. ContractIntegrationTest (krok 9)
```

## MockWebServer — dispaching requests

Klucz: MockWebServer jest kolejkowy (FIFO). Dla scenariuszy z wieloma endpointami (lista + N×single + N×files + N×reviews) trzeba enqueue'ować w dokładnej kolejności wywołań. Alternatywa: `Dispatcher` pattern:

```java
githubApi.setDispatcher(new Dispatcher() {
    @Override public MockResponse dispatch(RecordedRequest req) {
        String path = req.getPath();
        if (path.matches("/repos/.+/pulls\\?.*")) return fromFixture("pr-list-3.json");
        if (path.matches("/repos/.+/pulls/1$")) return fromFixture("pr-single-1.json");
        if (path.matches("/repos/.+/pulls/1/files.*")) return fromFixture("pr-files-1.json");
        if (path.matches("/repos/.+/pulls/1/reviews.*")) return fromFixture("pr-reviews-approved.json");
        // ... per PR
        return new MockResponse().setResponseCode(404);
    }
});
```

Dispatcher jest czystszy niż kolejkowanie — rekomendowany.

### Warstwa 5: Frontend E2E (Playwright)

11. **Instalacja Playwright** — `npm init playwright@latest` w `frontend/`:
    - Chromium only (headless, bez Firefox/WebKit — szybciej)
    - Config: `playwright.config.ts` z `baseURL: http://localhost:3000`, timeout 30s
    - Script w `package.json`: `"test:e2e": "playwright test"`

12. **Problem: backend musi działać podczas testów frontend**
    - Playwright `webServer` config — uruchamia backend + frontend automatycznie przed testami
    - Backend: `cd ../backend && mvn spring-boot:run -Dspring-boot.run.profiles=integration-github`
    - Frontend: `npm run dev`
    - Ale MockWebServer jest w JVM — nie zadziała z osobnym procesem backendu

    **Rozwiązanie alternatywne**: Backend z **WireMock standalone** (jar) zamiast MockWebServer.

    **Jeszcze prostsze rozwiązanie**: Playwright testuje przeciwko **prawdziwemu backendowi z H2** ale z `@MockBean` provider — jak istniejący `IntegrationTest.java`. Albo:

    **Rekomendowane rozwiązanie**: Playwright z **MSW (Mock Service Worker)** na frontendzie — mockuje API responses w przeglądarce, nie potrzebuje backendu. Testuje: renderowanie, nawigację, interakcje, filtrowanie — to co frontend robi z danymi.

13. **MSW setup** (`frontend/src/test/mocks/`):
    - `handlers.ts` — mock handlers dla `/api/activity/**`, `/api/browse`, `/api/repos`
    - `server.ts` — MSW setupServer dla testów
    - Realistyczne dane (te same co w backend fixtures)

14. **Testy Playwright** (`frontend/e2e/`):
    - `activity-dashboard.spec.ts` — US7: wybór repo → kontrybutor → stats + heatmapa + flagi
    - `analysis-dashboard.spec.ts` — US6: browse → analiza → wyniki
    - `navigation.spec.ts` — US8: nawigacja navbar, routing

## Kolejność implementacji

```
Phase 1 — Backend infra:     T01-T03 (profil, base class, fixtures)
Phase 2 — Backend testy:     T04-T08 (5 klas testowych, równolegle)
Phase 3 — Frontend infra:    T11-T13 (Playwright + MSW setup)
Phase 4 — Frontend E2E:      T14-T16 (3 pliki testów)
Phase 5 — Weryfikacja:       T09-T10
```

## Ryzyka

| Ryzyko | Mitigation |
|--------|------------|
| MockWebServer port conflict w CI | `server.start()` na random port + `@DynamicPropertySource` |
| Spring context per test class = wolne | Wspólna klasa bazowa, context caching przez Spring |
| Fixtures muszą być aktualne z GitHub API | Komentarz w fixture z datą i wersją API |
| Parallel fetch w ActivityAnalysisService + MockWebServer | Dispatcher zamiast FIFO queue |
| Playwright wymaga przeglądarki | Chromium only, headless, `npx playwright install chromium` |
| MSW + Vite compatibility | MSW 2.x wspiera Vite out of the box |
