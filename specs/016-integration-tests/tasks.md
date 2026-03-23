# 016: Tasks — Testy integracyjne E2E

## Phase 1: Infrastruktura

- [ ] T01 `application-integration-github.yml` — profil testowy (H2 mem, github api-url placeholder, token dummy, llm none)
- [ ] T02 `GitHubIntegrationTestBase.java` — klasa bazowa: MockWebServer, @DynamicPropertySource, helpers, BEZ @MockBean na providerach

## Phase 2: Fixtures

- [ ] T03 [P] GitHub JSON fixtures w `src/test/resources/github-fixtures/`:
  - `pr-list-3.json` — 3 PR-y BEZ additions/deletions (realistyczny GET /pulls)
  - `pr-single-1.json`, `pr-single-2.json`, `pr-single-3.json` — single PR-y Z additions/deletions
  - `pr-files-1.json`, `pr-files-2.json`, `pr-files-3.json` — pliki per PR
  - `pr-reviews-empty.json`, `pr-reviews-approved.json`, `pr-reviews-self.json`
  - `error-401.json`, `error-429.json`

## Phase 3: Testy integracyjne

- [ ] T04 `BrowseIntegrationTest` — browse flow z realistycznym GitHub API:
  - changedFilesCount=0 z listy (dokumentuje znany limit)
  - poprawne metadane (title, author, state, dates)
- [ ] T05 `AnalysisIntegrationTest` — analiza z realistycznym GitHub API:
  - 3 PR-y → 3 wyniki z score > 0
  - penalty za duży diff (PR #2)
  - boost za test files (PR #3)
- [ ] T06 `ActivityIntegrationTest` — activity dashboard z realistycznym API:
  - avgSize > 0 (weryfikuje bug z browse vs single PR)
  - contributors z 3 autorami
  - flaga "Brak review" gdy reviews empty
  - flaga "Podejrzanie szybki merge"
- [ ] T07 `GitHubErrorHandlingIntegrationTest` — error handling:
  - 401 → sensowny błąd REST
  - 429 → informacja o rate limit
  - 404 na reviews → graceful fallback
- [ ] T08 `GitHubContractIntegrationTest` — parsowanie pełnego JSON:
  - PR z wszystkimi polami → domain model poprawny
  - Reviews z różnymi state → ReviewInfo poprawne

## Phase 4: Frontend E2E — infrastruktura

- [ ] T11 Instalacja Playwright: `npm init playwright@latest` (Chromium only, headless)
  - `playwright.config.ts` — baseURL localhost:3000, timeout 30s
  - `package.json` script: `"test:e2e": "playwright test"`
- [ ] T12 Instalacja MSW 2.x: `npm install -D msw`
  - `frontend/src/test/mocks/handlers.ts` — mock handlers dla `/api/activity/**`, `/api/browse`, `/api/repos`, `/api/analysis`
  - `frontend/src/test/mocks/browser.ts` — setupWorker dla Playwright
  - Realistyczne dane w handlerach (kontrybutorzy, flagi, heatmapa, PRy)
- [ ] T13 MSW integration w Playwright — `beforeEach` startuje MSW w przeglądarce

## Phase 5: Frontend E2E — testy

- [ ] T14 `frontend/e2e/activity-dashboard.spec.ts`:
  - Wybór repo → lista kontrybutorów → wybór → statystyki widoczne
  - Heatmapa renderowana (SVG z rect)
  - Kliknięcie badge severity → filtrowanie tabeli
  - Kliknięcie heatmapy → drill-down
- [ ] T15 `frontend/e2e/analysis-dashboard.spec.ts`:
  - Wybór repo → lista PR-ów → zaznaczenie → analiza → wyniki ze score
  - Kliknięcie wiersza → detail page
- [ ] T16 `frontend/e2e/navigation.spec.ts`:
  - Navbar "Aktywność" → route `/activity`
  - Navbar "Analiza PR" → route `/`
  - Direct URL `/activity` → strona aktywności
  - 404 page → komunikat "nie znaleziona"

## Phase 6: Weryfikacja

- [ ] T09 Pełny `mvn test` — wszystkie testy backend (stare + nowe) przechodzą, 0 failures
- [ ] T10 `npx vitest run` + `npx playwright test` — frontend unit + E2E, 0 failures
- [ ] T17 Aktualizacja CLAUDE.md — sekcja o testach integracyjnych i E2E

## Dependencies

```
Backend: T01 + T03 (równolegle) → T02 → T04-T08 (równolegle) → T09
Frontend: T11 + T12 (równolegle) → T13 → T14-T16 (równolegle) → T10
Finalizacja: T09 + T10 → T17
```

Backend i frontend phases mogą iść równolegle.

## Notes

- Dispatcher pattern zamiast FIFO queue w MockWebServer (parallel fetch w ActivityAnalysisService)
- BEZ @MockBean na providerach w testach backend — pełny flow przez prawdziwe adaptery
- MSW w Playwright — mockuje API w przeglądarce, nie potrzebuje backendu
- Fixtures wzorowane na prawdziwych odpowiedziach GitHub API v3
