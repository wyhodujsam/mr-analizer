# Feature Specification: Historia analiz per PR z filtrowaniem

**Feature Branch**: `005-analysis-history-per-pr`
**Created**: 2026-03-21
**Status**: Draft
**Input**: Historia analiz powinna pokazywac konkretne PR (tytul, numer, autor, score, verdict) zamiast agregatow per raport. Dodac filtrowanie po repo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Historia analiz pokazuje konkretne PR (Priority: P1)

Jako uzytkownik chce widziec w historii analiz konkretne PR ktore zostaly przeanalizowane (numer, tytul, autor, score, verdict), zamiast agregatow (ile automatable/maybe/not suitable), abym wiedzial dokladnie co bylo analizowane.

**Why this priority**: Obecna historia jest bezuzyteczna — widac tylko "1 PR, 0 auto, 1 maybe" bez informacji ktory PR to byl. Kluczowe do pracy z narzedziem.

**Independent Test**: Przeanalizuj 2 rozne PR → w historii widac oba z tytulem, numerem, autorem, score i verdict.

**Acceptance Scenarios**:

1. **Given** uzytkownik przeanalizowal MR #5 "Refactor service", **When** przegladaa historie analiz, **Then** widzi wiersz: data, repo, #5, "Refactor service", autor, score, verdict (kolorowy badge), przycisk usun
2. **Given** uzytkownik przeanalizowal MR #5 i MR #8 z tego samego repo, **When** przegladaa historie, **Then** widzi oba PR jako oddzielne wiersze
3. **Given** analiza zawierala 3 zaznaczone PR (#1, #2, #3), **When** przegladaa historie, **Then** widzi 3 wiersze z jednym reportId ale roznymi PR
4. **Given** historia zawiera wiele wierszy, **When** uzytkownik klika na wiersz PR, **Then** otwiera sie strona szczegulow MR w nowej karcie

---

### User Story 2 - Filtrowanie historii po repozytorium (Priority: P1)

Jako uzytkownik chce filtrowac historie analiz po repozytorium, abym mogl szybko znalezc analizy konkretnego repo gdy mam wiele zapisanych repozytoriow.

**Why this priority**: Bez filtrowania historia staje sie nieczytelna przy wielu repo.

**Independent Test**: Przeanalizuj PR z 2 roznych repo → filtruj po jednym → widac tylko analizy tego repo.

**Acceptance Scenarios**:

1. **Given** historia zawiera analizy z "owner/repo-a" i "owner/repo-b", **When** uzytkownik wybiera filtr "owner/repo-a", **Then** widzi tylko analizy z repo-a
2. **Given** filtr jest ustawiony na "owner/repo-a", **When** uzytkownik wybiera "Wszystkie", **Then** widzi analizy ze wszystkich repo
3. **Given** dropdown filtra, **Then** zawiera opcje: "Wszystkie" + lista unikalnych repo z historii

---

### User Story 3 - Wykres kolowy podsumowania (Priority: P2)

Jako uzytkownik chce widziec wykres kolowy podsumowujacy ile PR jest ocenionych jako AUTOMATABLE / MAYBE / NOT_SUITABLE, abym mogl na pierwszy rzut oka ocenic potencjal automatyzacji.

**Why this priority**: Wizualizacja danych — uzywa istniejacych danych z SummaryCard, nie wymaga zmian w backendzie.

**Independent Test**: Przeanalizuj kilka PR → wykres kolowy pokazuje proporcje z kolorami (zielony/zolty/czerwony).

**Acceptance Scenarios**:

1. **Given** analiza zakonczona z 3 AUTOMATABLE, 5 MAYBE, 2 NOT_SUITABLE, **When** uzytkownik widzi dashboard, **Then** wykres kolowy pokazuje 3 segmenty z poprawnymi proporcjami i kolorami
2. **Given** analiza z 0 wynikow, **Then** wykres kolowy nie jest wyswietlany
3. **Given** wykres kolowy, **Then** kazdy segment ma etykiete z nazwa verdict i liczba

---

### Edge Cases

- Co jesli analiza zawierala 0 PR (puste selectedMrIds)? Nie pokazuj wiersza w historii.
- Co jesli raport zostanie usuniety? Wszystkie wiersze PR z tego raportu znikaja.
- Co jesli to samo PR zostalo przeanalizowane dwukrotnie? Oba wpisy widoczne z roznymi datami.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Historia analiz MUST wyswietlac wiersze per PR (nie per raport): numer PR, tytul, autor, score, verdict badge, data analizy, repo slug
- **FR-002**: Kazdy wiersz MUST miec przycisk "Usun" usuwajacy caly raport (nie pojedynczy PR)
- **FR-003**: Historia MUST miec dropdown filtra po repozytorium z opcja "Wszystkie"
- **FR-004**: Klikniecie na wiersz PR MUST otwierac szczegoly MR w nowej karcie
- **FR-005**: API GET /api/analysis MUST zwracac wyniki z nested results (nie puste jak dotychczas w trybie listowania)
- **FR-006**: System MUST wyswietlac wykres kolowy z podzialem PR na AUTOMATABLE/MAYBE/NOT_SUITABLE po zakonczeniu analizy
- **FR-007**: Wykres MUST uzywac kolorow: zielony (AUTOMATABLE), zolty (MAYBE), czerwony (NOT_SUITABLE)
- **FR-008**: Testy BDD pokrywajace nowe scenariusze (min. 3)
- **FR-009**: Testy jednostkowe (min. 3)

### Key Entities

Bez nowych encji — zmiana dotyczy sposobu wyswietlania istniejacych AnalysisReport + AnalysisResult.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Historia analiz pokazuje konkretne PR z tytulem, numerem i score zamiast agregatow
- **SC-002**: Filtr po repo dziala poprawnie — pokazuje tylko analizy wybranego repo
- **SC-003**: Testy BDD (min. 3 nowe scenariusze)
- **SC-004**: Testy jednostkowe (min. 3 nowe)
