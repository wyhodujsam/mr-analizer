# 016: Testy integracyjne E2E — weryfikacja pełnego flow z realistycznym GitHub API

**Feature Branch**: `016-integration-tests`
**Created**: 2026-03-22
**Status**: Draft

## Problem

Obecne testy mają lukę integracyjną:
- **Unit testy reguł** — mockują MergeRequest z ręcznie ustawionymi danymi → nie wyłapią że GitHub API nie zwraca `additions/deletions` na liście PR-ów
- **BDD testy** — mockują `MergeRequestProvider` → nie wyłapią że `toDomainWithoutFiles()` zwraca `DiffStats(0,0,0)` z listy
- **IntegrationTest.java** — mockuje `MergeRequestProvider` → testuje REST API round-trip ale pomija adapter GitHub
- **GitHubClientTest** — testuje HTTP client z MockWebServer ale **nie testuje mapowania DTO → domain → REST response**

Brakuje testów które weryfikują **pełny flow**: realistyczne odpowiedzi GitHub API (MockWebServer) → GitHubAdapter → domain service → REST API response. To właśnie taki test wyłapałby bug z `changedFilesCount=0`.

## Podejście

Testy integracyjne z **MockWebServer** symulującym GitHub API z realistycznymi odpowiedziami JSON. Pełny Spring Boot context (bez mocków providerów) — prawdziwy `GitHubAdapter` + `GitHubClient` + `GitHubMapper` + service + REST controller.

### Co NIE robimy
- Nie dodajemy Testcontainers — nie potrzebujemy prawdziwego GitHub
- Nie dodajemy Pact — za dużo setup na ten moment

## User Scenarios & Testing

### User Story 1 — E2E: Browse PR-ów z realistycznym GitHub API (Priority: P1)

Test pełnego flow: MockWebServer zwraca realistyczną odpowiedź GitHub → browse endpoint zwraca poprawne dane.

**Weryfikuje bug**: `changedFilesCount=0` z listy PR-ów (GitHub lista nie zwraca tych pól).

**Acceptance Scenarios**:

1. **Given** MockWebServer zwraca listę PR-ów BEZ pól `additions/deletions/changed_files`, **When** wywołuję browse, **Then** response ma `changedFilesCount=0` (potwierdza znany limit API).
2. **Given** MockWebServer zwraca single PR Z polami `additions/deletions/changed_files`, **When** wywołuję analizę, **Then** wyniki mają poprawne `changedFilesCount > 0`.

---

### User Story 2 — E2E: Analiza PR-ów z realistycznym GitHub API (Priority: P1)

Test pełnego flow analizy: browse → analiza → raport z poprawnymi score'ami i metadanymi.

**Acceptance Scenarios**:

1. **Given** MockWebServer zwraca 3 PR-y (lista) + pliki (per PR), **When** uruchamiam analizę, **Then** raport zawiera 3 wyniki z poprawnymi score/verdict.
2. **Given** MockWebServer zwraca PR z dużym diff (>500 linii), **When** uruchamiam analizę, **Then** wynik ma penalty za duży diff.
3. **Given** MockWebServer zwraca PR z plikami testowymi, **When** uruchamiam analizę, **Then** wynik ma boost za testy.

---

### User Story 3 — E2E: Activity dashboard z realistycznym GitHub API (Priority: P1)

Test modułu aktywności z realistycznym API — weryfikacja że `fetchMergeRequest` (single) daje poprawne `additions/deletions`.

**Acceptance Scenarios**:

1. **Given** MockWebServer zwraca listę PR-ów (bez additions) + single PR per request (z additions), **When** wywołuję activity report, **Then** `avgSize > 0` (nie 0 jak z browse).
2. **Given** MockWebServer zwraca reviews endpoint, **When** wywołuję activity report dla PR bez reviews, **Then** flaga "Brak review" jest obecna.
3. **Given** MockWebServer zwraca PR zmergowany w 2 minuty z 200 liniami, **When** wywołuję activity report, **Then** flaga "Podejrzanie szybki merge" jest obecna.

---

### User Story 4 — E2E: Obsługa błędów GitHub API (Priority: P2)

Weryfikacja graceful handling błędów w pełnym flow (nie tylko na poziomie GitHubClient).

**Acceptance Scenarios**:

1. **Given** MockWebServer zwraca 401 Unauthorized, **When** wywołuję browse, **Then** REST API zwraca sensowny błąd (nie 500 z stacktrace).
2. **Given** MockWebServer zwraca 429 Rate Limit, **When** wywołuję browse, **Then** REST API zwraca informację o rate limit.
3. **Given** MockWebServer zwraca 404 na repo, **When** wywołuję activity contributors, **Then** REST API zwraca 404 lub pusty wynik.

---

### User Story 5 — E2E: Kontraktowa zgodność z GitHub API (Priority: P2)

Weryfikacja że nasze DTO poprawnie parsują pola z realnej odpowiedzi GitHub API (realistyczny JSON z pełnymi nested obiektami).

**Acceptance Scenarios**:

1. **Given** MockWebServer zwraca pełny GitHub PR JSON (z user, head, base, labels, merged_at, html_url), **When** parsowany przez GitHubAdapter, **Then** domain MergeRequest ma wszystkie pola poprawnie zmapowane.
2. **Given** MockWebServer zwraca reviews JSON z różnymi state (APPROVED, CHANGES_REQUESTED, COMMENTED), **When** parsowany przez GitHubReviewAdapter, **Then** ReviewInfo ma poprawne state i reviewer.

## Requirements

### Functional Requirements

- **FR-001**: Testy MUSZĄ używać pełnego Spring Boot context z prawdziwymi adapterami (bez `@MockBean` na providerach)
- **FR-002**: GitHub API symulowany przez MockWebServer z realistycznymi JSON-ami
- **FR-003**: Testy MUSZĄ weryfikować cały łańcuch: HTTP response → DTO → domain model → REST response
- **FR-004**: Testy MUSZĄ pokrywać znane bugi (changedFilesCount=0 z listy)
- **FR-005**: Testy MUSZĄ być powtarzalne i deterministyczne (brak zależności od zewnętrznych serwisów)
- **FR-006**: Testy MUSZĄ działać w CI bez tokenu GitHub
- **FR-007**: Frontend E2E testy MUSZĄ używać Playwright w trybie headless
- **FR-008**: Frontend E2E testy MUSZĄ uruchamiać backend (z MockWebServer) jako fixture
- **FR-009**: Frontend E2E MUSZĄ weryfikować renderowanie danych, nie tylko obecność elementów

## Technical Approach

### Profil testowy `integration-github`

Osobny profil Spring Boot (`application-integration-github.yml`) wskazujący `github.api-url` na MockWebServer. Token: dummy. LLM: `none`.

### Klasa bazowa `GitHubIntegrationTestBase`

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("integration-github")
abstract class GitHubIntegrationTestBase {
    static MockWebServer githubApi;
    @Autowired TestRestTemplate restTemplate;

    @BeforeAll — start MockWebServer
    @AfterAll — shutdown
    @DynamicPropertySource — override github.api-url + github.token
}
```

Brak `@MockBean` na `MergeRequestProvider` / `ReviewProvider` — prawdziwe adaptery.

### Realistyczne JSON fixtures

Pliki `src/test/resources/github-fixtures/`:
- `pr-list.json` — lista PR-ów BEZ additions/deletions (jak prawdziwe GitHub API)
- `pr-single-1.json` — single PR Z additions/deletions
- `pr-files-1.json` — lista plików PR-a
- `pr-reviews-1.json` — lista reviews
- `error-401.json`, `error-429.json`

### Struktura testów

```
backend/src/test/java/com/mranalizer/integration/
  GitHubIntegrationTestBase.java          # bazowa klasa z MockWebServer
  BrowseIntegrationTest.java              # US1: browse flow
  AnalysisIntegrationTest.java            # US2: analysis flow
  ActivityIntegrationTest.java            # US3: activity flow
  GitHubErrorHandlingIntegrationTest.java # US4: error handling
  GitHubContractIntegrationTest.java      # US5: contract test
```

### User Story 6 — Frontend E2E: Dashboard analiza PR (Priority: P1)

Playwright testuje pełny flow w przeglądarce: frontend ↔ backend (z MockWebServer jako GitHub API).

**Acceptance Scenarios**:

1. **Given** aplikacja uruchomiona (backend + frontend), **When** wpisuję repo i klikam "Wybierz", **Then** widzę listę PR-ów do przeglądania.
2. **Given** lista PR-ów widoczna, **When** zaznaczam PR-y i klikam "Analizuj", **Then** widzę wyniki ze score i verdict.

---

### User Story 7 — Frontend E2E: Dashboard aktywności (Priority: P1)

Playwright testuje flow aktywności: wybór repo → kontrybutor → dashboard z heatmapą.

**Acceptance Scenarios**:

1. **Given** aplikacja uruchomiona, **When** przechodzę na `/activity`, wybieram repo i kontrybutora, **Then** widzę statystyki i heatmapę.
2. **Given** dashboard aktywności załadowany, **When** klikam kwadrat heatmapy z aktywnością, **Then** widzę drill-down z listą PR-ów.
3. **Given** dashboard z flagami, **When** klikam badge severity, **Then** tabela filtruje się.

---

### User Story 8 — Frontend E2E: Nawigacja (Priority: P2)

Weryfikacja routingu i nawigacji między stronami.

**Acceptance Scenarios**:

1. **Given** strona główna, **When** klikam "Aktywność" w navbar, **Then** przechodzę na `/activity`.
2. **Given** strona aktywności, **When** klikam "Analiza PR" w navbar, **Then** wracam na `/`.

## Poza zakresem

- Testy wydajnościowe (JMeter/k6)
- Pact contract testing
- Testy z prawdziwym GitHub API
- Visual regression testing

## Success Criteria

- **SC-001**: Test wyłapujący bug `changedFilesCount=0` z browse przechodzi
- **SC-002**: Test wyłapujący bug `avgSize=0` w activity przechodzi
- **SC-003**: Pełny `mvn test` przechodzi bez tokenu GitHub
- **SC-004**: Żaden test integracyjny nie wymaga `@MockBean` na providerach
