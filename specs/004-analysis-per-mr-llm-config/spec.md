# Feature Specification: Analiza per MR + konfigurowalny prompt LLM

**Feature Branch**: `004-analysis-per-mr-llm-config`
**Created**: 2026-03-21
**Status**: Draft
**Input**: 1) Analiza per MR zamiast per repo — cache nie blokuje kolejnych analiz. 2) Konfigurowalny prompt i szablon odpowiedzi LLM.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Niezalezne analizy per MR (Priority: P1)

Jako uzytkownik chce analizowac dowolne MR z tego samego repozytorium niezaleznie od siebie, abym mogl analizowac rozne PR po kolei bez koniecznosci usuwania poprzednich analiz.

**Why this priority**: Aktualny cache per repo blokuje mozliwosc analizy kolejnych MR — to krytyczny bug uniemozliwiajacy normalna prace.

**Independent Test**: Analizuj MR #1 z repo X, potem MR #3 z repo X — obie analizy powinny istniec niezaleznie.

**Acceptance Scenarios**:

1. **Given** analiza MR #1 z repo "owner/repo" istnieje, **When** uzytkownik uruchomi analize MR #3 z tego samego repo, **Then** system tworzy nowa analize dla MR #3 bez nadpisywania analizy MR #1
2. **Given** dwie analizy z tego samego repo istnieja (MR #1 i MR #3), **When** uzytkownik przegla historie analiz, **Then** obie analizy sa widoczne na liscie
3. **Given** analiza MR #1 istnieje, **When** uzytkownik usunie analize MR #1, **Then** analiza MR #3 pozostaje nienaruszona
4. **Given** uzytkownik zaznaczyl checkboxami MR #1 i #2, **When** uruchomi analize, **Then** system tworzy jedna analize zawierajaca wyniki dla obu zaznaczonych MR
5. **Given** analiza dla MR #1,#2 juz istnieje, **When** uzytkownik zaznacza MR #1,#3 i uruchomi analize, **Then** system tworzy NOWA analize (nie uzywa cache bo lista MR sie rozni)

---

### User Story 2 - Konfigurowalny prompt LLM (Priority: P1)

Jako uzytkownik chce moc skonfigurowac prompt wysylany do LLM oraz szablon oczekiwanej odpowiedzi, abym mogl dostrujc analize do moich potrzeb (np. zmiana kryteriow oceny, jezyk odpowiedzi, format).

**Why this priority**: Hardcoded prompt ogranicza mozliwosc dostosowania narzedzia. Uzytkownik powinien moc zdefiniowac co LLM ma oceniac i jak odpowiadac.

**Independent Test**: Zmien prompt w konfiguracji, uruchom analize z LLM — odpowiedz powinna odpowiadac nowemu promptowi.

**Acceptance Scenarios**:

1. **Given** uzytkownik zmienil prompt w application.yml, **When** uruchomi analize z LLM, **Then** Claude CLI otrzymuje nowy prompt zamiast domyslnego
2. **Given** prompt zawiera placeholdery (np. `{{title}}`, `{{description}}`, `{{filesChanged}}`), **When** system buduje prompt, **Then** placeholdery sa zastepowane danymi MR
3. **Given** szablon odpowiedzi definiuje oczekiwane pola JSON, **When** LLM zwroci odpowiedz, **Then** system parsuje ja zgodnie z szablonem
4. **Given** uzytkownik nie zmienil konfiguracji promptu, **When** uruchomi analize z LLM, **Then** system uzywa domyslnego promptu (zachowanie wsteczne kompatybilne)

---

### Edge Cases

- Co jesli prompt jest pusty w konfiguracji? System uzywa domyslnego promptu.
- Co jesli placeholder w prompcie nie istnieje (np. `{{nonExistentField}}`)? System zostawia go jako pusty string i loguje warning.
- Co jesli LLM zwroci odpowiedz w formacie niezgodnym z szablonem? System fallbackuje na neutralny LlmAssessment (jak przy timeout).
- Co jesli uzytkownik analizuje 0 MR (puste selectedMrIds)? System zwraca pusty raport.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST umozliwiac tworzenie wielu niezaleznych analiz dla MR z tego samego repozytorium
- **FR-002**: System MUST NIE blokowac nowej analizy gdy istnieje juz analiza dla tego samego repo
- **FR-003**: Kazda analiza MUST byc identyfikowana unikatowo (reportId) niezaleznie od repo
- **FR-004**: System MUST umozliwiac konfiguracje promptu LLM w application.yml
- **FR-005**: Prompt MUST obslugiwac placeholdery: `{{title}}`, `{{description}}`, `{{filesChanged}}`, `{{additions}}`, `{{deletions}}`, `{{hasTests}}`, `{{labels}}`, `{{author}}`, `{{sourceBranch}}`, `{{targetBranch}}`
- **FR-006**: System MUST umozliwiac konfiguracje szablonu oczekiwanej odpowiedzi JSON (nazwy pol do parsowania)
- **FR-007**: System MUST zachowac wsteczna kompatybilnosc — domyslny prompt i szablon gdy brak konfiguracji
- **FR-008**: System MUST miec testy BDD pokrywajace nowe scenariusze
- **FR-009**: System MUST miec testy jednostkowe dla cache logic i prompt builder

### Key Entities

- **LlmPromptConfig**: Konfiguracja promptu — template string z placeholderami, nazwy pol odpowiedzi (scoreField, commentField)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uzytkownik moze utworzyc 3+ niezalezne analizy dla tego samego repo bez blokowania
- **SC-002**: Zmiana promptu w application.yml zmienia tresc wysylana do Claude CLI
- **SC-003**: Testy BDD pokrywaja nowe scenariusze (min. 5 nowych)
- **SC-004**: Testy jednostkowe (min. 8 nowych test cases)
