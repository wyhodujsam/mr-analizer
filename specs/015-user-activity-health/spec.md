# 015: Aktywność użytkownika w repozytorium — wykrywanie nieprawidłowości


**Feature Branch**: `015-user-activity-health`
**Created**: 2026-03-22
**Status**: Draft
**Input**: Nowa funkcjonalność odseparowana od analizy PR/MR przez LLM — analiza aktywności użytkownika w repozytorium, wykrywanie nieprawidłowości (za duże PR-y, krótki review, commity w weekend).

## Problem

Aplikacja mr_analizer analizuje PR-y pod kątem automatyzowalności. Brakuje narzędzia do **monitorowania zdrowia procesu wytwórczego** — patternów, które wskazują na:
- **Za duże PR-y** — trudne do review, ryzyko regresji
- **Zbyt krótki czas review** — rubber-stamping, brak realnej kontroli jakości
- **Commity w weekendy / poza godzinami** — ryzyko wypalenia, niska produktywność
- **Brak review na PR-ach** — self-merge, omijanie procesu
- **Nierównomierne obciążenie** — jedna osoba robi 80% commitów

Funkcjonalność jest **odseparowana** od istniejącej analizy LLM — osobny moduł domenowy, osobne endpointy, osobna strona.

## User Scenarios & Testing

### User Story 1 — Dashboard aktywności użytkownika (Priority: P1)

Użytkownik (tech lead / engineering manager) chce zobaczyć dashboard aktywności wybranego kontrybutora w repozytorium, aby wychwycić nieprawidłowości w procesie wytwórczym.

**Why this priority**: Podstawowa wartość — bez dashboardu nie ma czego pokazywać.

**Independent Test**: Można przetestować niezależnie — wybierz repo + użytkownika → widzisz dashboard z metrykami i flagami.

**Acceptance Scenarios**:

1. **Given** zapisane repozytorium z historią PR-ów, **When** użytkownik wybiera kontrybutora z listy i klika "Pokaż aktywność", **Then** system wyświetla dashboard z metrykami i wykrytymi nieprawidłowościami.
2. **Given** kontrybutor bez żadnych nieprawidłowości, **When** dashboard się ładuje, **Then** widać metryki ale sekcja flag jest pusta z komunikatem "Brak wykrytych nieprawidłowości".
3. **Given** kontrybutor z wieloma flagami, **When** dashboard się ładuje, **Then** flagi posortowane są wg severity (critical > warning > info).

---

### User Story 2 — Wykrywanie za dużych PR-ów (Priority: P1)

System automatycznie flaguje PR-y, które przekraczają progi wielkości.

**Why this priority**: Najłatwiejszy do wykrycia pattern — dane (additions/deletions) już mamy w GitHub API.

**Independent Test**: Analizuj repo z mieszanką małych i dużych PR-ów → flagi pojawiają się tylko przy dużych.

**Acceptance Scenarios**:

1. **Given** PR ze zmianami >500 linii (additions+deletions), **When** system analizuje aktywność, **Then** PR oznaczony flagą "Za duży PR" (severity: warning).
2. **Given** PR ze zmianami >1000 linii, **When** system analizuje aktywność, **Then** PR oznaczony flagą "Bardzo duży PR" (severity: critical).
3. **Given** PR ze zmianami <500 linii, **When** system analizuje aktywność, **Then** brak flagi dotyczącej wielkości.

---

### User Story 3 — Wykrywanie zbyt krótkiego review (Priority: P1)

System flaguje PR-y, gdzie czas od utworzenia do merge jest podejrzanie krótki.

**Why this priority**: Krótki review = rubber-stamping, poważne ryzyko jakości.

**Independent Test**: Analizuj repo z PR-ami o różnym czasie review → flagi przy krótkich.

**Acceptance Scenarios**:

1. **Given** PR zmergowany w <10 minut od utworzenia ze zmianami >50 linii, **When** system analizuje aktywność, **Then** PR oznaczony flagą "Zbyt krótki review" (severity: warning).
2. **Given** PR zmergowany w <5 minut od utworzenia ze zmianami >100 linii, **When** system analizuje aktywność, **Then** flaga "Podejrzanie szybki merge" (severity: critical).
3. **Given** mały PR (<50 linii) zmergowany szybko, **When** system analizuje aktywność, **Then** brak flagi (małe PR-y mogą być mergowane szybko).

---

### User Story 4 — Wykrywanie pracy poza godzinami (Priority: P2)

System flaguje commity/merge w weekendy i późno w nocy.

**Why this priority**: Ważny sygnał wypalenia, ale mniej krytyczny niż problemy z jakością procesu.

**Independent Test**: Analizuj repo z commitami w weekend → flagi pojawiają się przy weekendowych/nocnych.

**Acceptance Scenarios**:

1. **Given** PR utworzony/zmergowany w sobotę lub niedzielę, **When** system analizuje aktywność, **Then** PR oznaczony flagą "Praca w weekend" (severity: info).
2. **Given** >30% PR-ów użytkownika w danym miesiącu to weekendowe, **When** system wyświetla dashboard, **Then** widać podsumowującą flagę "Wysoki odsetek pracy weekendowej — ryzyko wypalenia" (severity: warning).
3. **Given** PR utworzony w nocy (22:00-06:00), **When** system analizuje aktywność, **Then** PR oznaczony flagą "Praca nocna" (severity: info).

---

### User Story 5 — Wykrywanie braku review / self-merge (Priority: P2)

System flaguje PR-y bez review lub zmergowane przez autora.

**Why this priority**: Self-merge = brak kontroli jakości. Wymaga dodatkowego API call (reviews).

**Independent Test**: Analizuj repo z PR-ami self-merge → flagi.

**Acceptance Scenarios**:

1. **Given** PR bez żadnego review (0 reviews), **When** system analizuje aktywność, **Then** PR oznaczony flagą "Brak review" (severity: warning).
2. **Given** PR zmergowany przez autora (self-merge), **When** system analizuje aktywność, **Then** PR oznaczony flagą "Self-merge" (severity: warning).

---

### User Story 6 — Heatmapa aktywności (Priority: P2)

Dashboard zawiera heatmapę w stylu GitHub contribution graph — siatka kwadratów (kolumny = tygodnie, wiersze = dni tygodnia) kolorowana intensywnością aktywności (liczba PR-ów utworzonych/zmergowanych danego dnia).

**Why this priority**: Wizualna reprezentacja aktywności pozwala natychmiast zobaczyć wzorce — weekendowa praca, nierównomierność, okresy nieaktywności.

**Independent Test**: Analizuj użytkownika z historią → widzisz heatmapę z kolorowymi kwadratami odpowiadającymi dniom z aktywnością.

**Acceptance Scenarios**:

1. **Given** kontrybutor z PR-ami rozłożonymi na 3 miesiące, **When** dashboard się ładuje, **Then** heatmapa pokazuje siatkę ~13 tygodni × 7 dni, kolorowaną wg liczby PR-ów danego dnia (0 = szary, 1 = jasny zielony, 2+ = ciemny zielony).
2. **Given** kontrybutor z PR-ami w weekendy, **When** patrzę na heatmapę, **Then** wiersze Sob/Niedz mają wyraźnie zaznaczone kolorowe kwadraty — widać wzorzec pracy weekendowej.
3. **Given** kontrybutor z PR-ami tylko w dni robocze, **When** patrzę na heatmapę, **Then** wiersze Sob/Niedz są szare (puste), Pon-Pt kolorowe.
4. **Given** hover nad kwadratem z aktywnością, **When** najadę myszką, **Then** widzę tooltip z datą i liczbą PR-ów (np. "2026-03-15: 2 PR-y").
5. **Given** kliknięcie kwadratu z aktywnością, **When** kliknę, **Then** pod heatmapą pojawia się lista PR-ów z tego dnia (tytuł, rozmiar, flagi).

---

### User Story 7 — Podsumowanie i statystyki (Priority: P2)

Dashboard pokazuje zagregowane statystyki kontrybutora.

**Why this priority**: Kontekst dla flag — bez statystyk flagi są wyrwane z kontekstu.

**Independent Test**: Analizuj użytkownika → widzisz statystyki (średni rozmiar PR, średni czas review, % weekendowych).

**Acceptance Scenarios**:

1. **Given** kontrybutor z historią PR-ów, **When** dashboard się ładuje, **Then** widać: liczbę PR-ów, średni rozmiar (linie), średni czas do merge, % PR-ów weekendowych, liczbę flag per severity.
2. **Given** kontrybutor bez PR-ów w wybranym okresie, **When** dashboard się ładuje, **Then** komunikat "Brak aktywności w wybranym okresie".

---

### Edge Cases

- Użytkownik nie istnieje w repozytorium (0 PR-ów) → komunikat "Nie znaleziono aktywności"
- PR-y bez daty merge (otwarte/zamknięte bez merge) → pomijane w analizie czasu review
- Repozytorium z jednym kontrybutorem → lista ma jednego użytkownika, self-merge nie jest flagowany (jedyny contributor)
- Strefa czasowa — godziny weekendowe/nocne w UTC (domyślnie), bez konfiguracji timezone w P1

## Requirements

### Functional Requirements

- **FR-001**: System MUSI pobierać listę kontrybutorów repozytorium (unikalni autorzy PR-ów)
- **FR-002**: System MUSI pobierać historię PR-ów danego kontrybutora z GitHub API (z paginacją)
- **FR-003**: System MUSI analizować PR-y pod kątem reguł nieprawidłowości (rozmiar, czas review, godziny pracy, brak review)
- **FR-004**: System MUSI wyświetlać wyniki na dedykowanej stronie odseparowanej od analizy LLM
- **FR-005**: System MUSI zwracać zagregowane statystyki (średni rozmiar, średni czas, % weekendowe)
- **FR-006**: System MUSI klasyfikować flagi wg severity: critical, warning, info
- **FR-007**: Moduł MUSI być odseparowany od istniejącej logiki analizy PR/MR — osobny pakiet domenowy, osobne endpointy REST, osobna strona frontend
- **FR-010**: System MUSI wyświetlać heatmapę aktywności (siatka tygodnie × dni, kolorowana wg liczby PR-ów danego dnia)
- **FR-011**: Heatmapa MUSI obsługiwać tooltip z datą i liczbą PR-ów przy hover
- **FR-012**: Heatmapa MUSI obsługiwać kliknięcie kwadratu — pokazanie listy PR-ów z danego dnia
- **FR-008**: System MUSI pobierać dane o reviews z GitHub API (nowe: reviews endpoint)
- **FR-009**: System MUSI obsługiwać paginację i rate-limiting GitHub API

### Key Entities

- **ActivityReport** — wynik analizy aktywności: kontrybutor, repo, okres, lista flag, statystyki
- **ActivityFlag** — pojedyncza nieprawidłowość: typ, severity, opis, odniesienie do PR
- **ContributorStats** — zagregowane statystyki: średni rozmiar PR, średni czas review, % weekendowe, liczba PR-ów
- **ActivityRule** — reguła wykrywania nieprawidłowości (interfejs): sprawdza PR i zwraca opcjonalną flagę
- **DailyActivity** — aktywność w danym dniu: data, liczba PR-ów, lista PR-ów (do heatmapy i drill-down)

## Technical Approach

### Separacja od istniejącego modułu

```
backend/src/main/java/com/mranalizer/
  domain/
    model/activity/          # ActivityReport, ActivityFlag, ContributorStats
    port/in/activity/        # ActivityAnalysisUseCase
    port/out/activity/       # (reużywa MergeRequestProvider + nowy ReviewProvider)
    service/activity/        # ActivityAnalysisService, reguły
  adapter/
    in/web/activity/         # ActivityController — /api/activity/**
    out/provider/github/     # Rozszerzenie GitHubClient o reviews endpoint

frontend/src/
  pages/ActivityDashboard/   # Osobna strona
  components/activity/       # Komponenty dashboardu
```

### Reużycie vs nowe

- **Reużywane**: `MergeRequestProvider` (fetch PR-ów), `GitHubClient` (rozszerzony o reviews), `FetchCriteria`
- **Nowe**: moduł domenowy `activity/`, reguły wykrywania, REST controller, strona frontend

### Reguły wykrywania (Strategy pattern)

Każda reguła to osobna klasa implementująca `ActivityRule`:
- `LargePrRule` — rozmiar PR-a (progi: 500 warning, 1000 critical)
- `QuickReviewRule` — czas review vs rozmiar (progi zależne od rozmiaru)
- `WeekendWorkRule` — dzień tygodnia create/merge
- `NightWorkRule` — godzina create/merge (22:00-06:00)
- `NoReviewRule` — brak reviews na PR
- `SelfMergeRule` — autor = merger

### GitHub API — nowe endpointy

- `GET /repos/{owner}/{repo}/pulls/{number}/reviews` — lista reviews (potrzebne dla NoReviewRule, SelfMergeRule)
- Istniejące endpointy wystarczają dla pozostałych reguł (rozmiar, daty)

### Frontend — nowa strona

- Route: `/activity/:projectSlug` (np. `/activity/owner/repo`)
- Komponenty: selektor kontrybutora, karty statystyk, tabela flag, heatmapa aktywności
- Heatmapa: pure CSS/SVG grid (bez zewnętrznej biblioteki), styl GitHub contribution graph
  - Kolumny = tygodnie, wiersze = dni (Pon-Niedz), kwadraty kolorowane wg intensywności
  - 4-5 poziomów koloru (szary → jasny zielony → ciemny zielony)
  - Tooltip na hover (data + liczba PR-ów), kliknięcie → drill-down lista PR-ów

## Poza zakresem

- Konfiguracja stref czasowych (domyślnie UTC)
- Konfiguracja progów reguł przez UI (hardcoded, zmiana w kodzie)
- Porównywanie kontrybutorów na jednym widoku
- Powiadomienia / alerty (email, Slack)
- Analiza commitów (tylko PR-level)
- GitLab adapter (tylko GitHub)
- Persystencja raportów aktywności (in-memory, odświeżane na żądanie)
- Integracja z analizą LLM

## Success Criteria

### Measurable Outcomes

- **SC-001**: Dashboard ładuje się w <5s dla repozytorium z 200 PR-ami
- **SC-002**: Wszystkie 6 reguł wykrywania działają poprawnie (pokryte testami BDD)
- **SC-003**: Moduł jest w pełni odseparowany — usunięcie go nie wpływa na istniejącą funkcjonalność analizy LLM
- **SC-004**: 90%+ pokrycie testami nowego kodu (unit + BDD)
- **SC-005**: Nawigacja do dashboardu aktywności dostępna z głównej strony

## Acceptance Scenarios (BDD)

### Feature: Analiza aktywności kontrybutora

```gherkin
Feature: Analiza aktywności kontrybutora w repozytorium

  Background:
    Given repozytorium "owner/repo" z wieloma kontrybutorami

  Scenario: Dashboard z metrykami i flagami
    Given kontrybutor "jan.kowalski" z 15 PR-ami
    And 3 PR-y mają >500 linii zmian
    And 1 PR został zmergowany w 3 minuty z 200 liniami zmian
    When analizuję aktywność "jan.kowalski"
    Then widzę 15 PR-ów w statystykach
    And widzę 3 flagi "Za duży PR" (severity: warning)
    And widzę 1 flagę "Podejrzanie szybki merge" (severity: critical)

  Scenario: Praca w weekend
    Given kontrybutor "anna.nowak" z 10 PR-ami
    And 4 PR-y zostały utworzone w sobotę lub niedzielę
    When analizuję aktywność "anna.nowak"
    Then widzę 4 flagi "Praca w weekend" (severity: info)
    And widzę flagę "Wysoki odsetek pracy weekendowej" (severity: warning)

  Scenario: Brak nieprawidłowości
    Given kontrybutor "piotr.zielinski" z 8 PR-ami
    And wszystkie PR-y mają <500 linii
    And wszystkie PR-y miały review trwający >30 minut
    And żaden PR nie był weekendowy
    When analizuję aktywność "piotr.zielinski"
    Then widzę 8 PR-ów w statystykach
    And sekcja flag jest pusta z komunikatem "Brak wykrytych nieprawidłowości"

  Scenario: Self-merge
    Given kontrybutor "jan.kowalski" z PR #42
    And PR #42 nie ma żadnych reviews
    When analizuję aktywność "jan.kowalski"
    Then widzę flagę "Brak review" na PR #42 (severity: warning)

  Scenario: Brak aktywności
    Given kontrybutor "ghost" z 0 PR-ami
    When analizuję aktywność "ghost"
    Then widzę komunikat "Nie znaleziono aktywności"

  Scenario: Heatmapa aktywności
    Given kontrybutor "jan.kowalski" z PR-ami w dniach: 2026-03-02, 2026-03-02, 2026-03-05, 2026-03-15
    When analizuję aktywność "jan.kowalski"
    Then heatmapa pokazuje kwadrat 2026-03-02 w ciemnym kolorze (2 PR-y)
    And kwadrat 2026-03-05 w jasnym kolorze (1 PR)
    And kwadrat 2026-03-15 w jasnym kolorze (1 PR)
    And pozostałe kwadraty są szare (0 PR-ów)

  Scenario: Heatmapa drill-down
    Given kontrybutor "jan.kowalski" z 2 PR-ami w dniu 2026-03-02
    When klikam kwadrat 2026-03-02 na heatmapie
    Then widzę listę 2 PR-ów z tego dnia z tytułami i rozmiarami
```

### Feature: REST API aktywności

```gherkin
Feature: REST API aktywności kontrybutora

  Scenario: Pobranie listy kontrybutorów
    Given repozytorium "owner/repo" z PR-ami od 3 różnych autorów
    When wywołuję GET /api/activity/owner/repo/contributors
    Then otrzymuję listę 3 kontrybutorów z liczbą PR-ów każdego

  Scenario: Pobranie raportu aktywności
    Given kontrybutor "jan.kowalski" z historią PR-ów
    When wywołuję GET /api/activity/owner/repo/report?author=jan.kowalski
    Then otrzymuję JSON z polami: stats, flags, pullRequests
    And stats zawiera: totalPrs, avgSize, avgReviewTime, weekendPercentage
    And flags jest posortowane wg severity (critical first)
    And response zawiera dailyActivity — mapa data→liczba PR-ów (dane do heatmapy)

  Scenario: Repozytorium nie istnieje
    When wywołuję GET /api/activity/nonexistent/repo/contributors
    Then otrzymuję HTTP 404
```
