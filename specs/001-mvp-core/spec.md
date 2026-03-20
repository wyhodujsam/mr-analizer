# Feature Specification: MVP Core

**Feature Branch**: `001-mvp-core`
**Created**: 2026-03-20
**Status**: Draft
**Input**: MVP — inicjalizacja projektu, model domenowy, silnik regul, adapter GitHub, Claude CLI, REST API, dashboard React, testy BDD

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Analiza repozytorium GitHub (Priority: P1)

Jako uzytkownik chce podac nazwe repozytorium GitHub, wybrac zakres dat i uruchomic analize, aby zobaczyc ktore Pull Requesty moglyby byc wykonane automatycznie przez LLM.

**Why this priority**: To jest rdzen aplikacji — bez mozliwosci pobrania PR z GitHub i ich analizy cala aplikacja nie ma sensu. Dostarcza natychmiastowa wartosc: uzytkownik widzi ranking PR pod katem automatyzacji.

**Independent Test**: Mozna przetestowac podajac dowolne publiczne repozytorium GitHub, uruchamiajac analize i weryfikujac ze wyniki zawieraja liste PR z ocenami.

**Acceptance Scenarios**:

1. **Given** uzytkownik otwiera dashboard, **When** wpisuje "owner/repo" jako project slug, wybiera zakres dat i klika "Analizuj", **Then** system pobiera PR z GitHub i wyswietla tabele wynikow z kolumnami: numer, tytul, autor, score, verdict
2. **Given** analiza zostala uruchomiona, **When** system pobierze PR z GitHub, **Then** kazdy PR otrzymuje score (0.0-1.0) i verdict (AUTOMATABLE, MAYBE, NOT_SUITABLE)
3. **Given** tabela wynikow jest wyswietlona, **When** uzytkownik klika na wiersz PR, **Then** system wyswietla strone szczegulow z pelnym score breakdown (ktore reguly zadzialaly i z jakim efektem)
4. **Given** repozytorium nie istnieje lub token jest nieprawidlowy, **When** uzytkownik uruchomi analize, **Then** system wyswietla czytelny komunikat bledu

---

### User Story 2 - Ocena PR przez silnik regul (Priority: P1)

Jako uzytkownik chce aby system automatycznie ocenial PR na podstawie konfigurowalnych regul (exclude/boost/penalize), abym mogl szybko zidentyfikowac kandydatow do automatyzacji.

**Why this priority**: Silnik regul jest sercem logiki biznesowej. Bez niego nie ma scoringu ani verdictow. Rownorzedny z US1 bo oba sa niezbedne.

**Independent Test**: Mozna przetestowac podajac dane PR (liczba plikow, labels, opis) i weryfikujac ze silnik zwraca prawidlowy score i verdict zgodnie z regulami.

**Acceptance Scenarios**:

1. **Given** PR z labelem "hotfix", **When** system ewaluuje reguly, **Then** PR jest wykluczony (score=0, verdict=NOT_SUITABLE) z powodem "excluded by label rule"
2. **Given** PR z tytulem zawierajacym "refactor" i 5 zmienionymi plikami z testami, **When** system ewaluuje reguly, **Then** score jest powyzej progu automatable (0.7+) dzieki boost rules
3. **Given** PR z diffem powyzej 500 linii i bez opisu, **When** system ewaluuje reguly, **Then** score jest obnizony przez penalize rules
4. **Given** PR z 1 zmienionym plikiem, **When** system ewaluuje reguly, **Then** PR jest wykluczony (za maly MR, ponizej min-changed-files)
5. **Given** PR ze zmianami wylacznie w plikach .yml i .toml, **When** system ewaluuje reguly, **Then** PR jest wykluczony (file-extensions-only rule)

---

### User Story 3 - Wzbogacenie analizy przez LLM (Priority: P2)

Jako uzytkownik chce opcjonalnie wlaczyc analize PR przez Claude CLI, aby uzyskac dodatkowa ocene oparta na zrozumieniu tresci zmian, a nie tylko metadanych.

**Why this priority**: LLM dodaje wartosc ponad reguly, ale system jest uzyteczny tez bez niego (NoOp adapter). Dlatego priorytet P2.

**Independent Test**: Mozna przetestowac wlaczajac adapter Claude CLI, analizujac PR i weryfikujac ze wynik zawiera komentarz LLM z uzasadnieniem.

**Acceptance Scenarios**:

1. **Given** adapter LLM ustawiony na "claude-cli", **When** system analizuje PR, **Then** Claude CLI otrzymuje prompt z danymi PR i zwraca ocene (score adjustment + uzasadnienie) ktora jest dolaczana do wyniku
2. **Given** adapter LLM ustawiony na "none", **When** system analizuje PR, **Then** analiza opiera sie wylacznie na silniku regul, bez wywolania LLM
3. **Given** Claude CLI nie odpowiada w ciagu 60 sekund, **When** system analizuje PR, **Then** analiza kontynuuje sie bez LLM z adnotacja "LLM timeout"

---

### User Story 4 - Dashboard z podsumowaniem (Priority: P2)

Jako uzytkownik chce widziec podsumowanie wynikow analizy na dashboardzie (ile PR automatable/maybe/not suitable), abym mogl szybko ocenic potencjal automatyzacji w repozytorium.

**Why this priority**: Podsumowanie dostarcza wartosc informacyjna, ale wymaga najpierw dzialajacego US1+US2. Priorytet P2.

**Independent Test**: Mozna przetestowac po zakonczeniu analizy — dashboard powinien wyswietlac karty z liczbami i procentami per verdict.

**Acceptance Scenarios**:

1. **Given** analiza zakonczona z wynikami, **When** uzytkownik widzi dashboard, **Then** wyswietlane sa karty podsumowania: liczba i procent PR dla kazdego verdict (AUTOMATABLE, MAYBE, NOT_SUITABLE)
2. **Given** tabela wynikow, **When** uzytkownik przegladaa wyniki, **Then** wiersze sa kolorowane: zielony (AUTOMATABLE), zolty (MAYBE), czerwony (NOT_SUITABLE)

---

### User Story 5 - Persystencja wynikow (Priority: P3)

Jako uzytkownik chce aby wyniki analiz byly zapisywane w bazie danych, abym mogl wrocic do nich pozniej bez ponownego uruchamiania analizy.

**Why this priority**: Persystencja to wygoda, nie rdzen. System dziala bez niej (wyniki w pamieci). Priorytet P3.

**Independent Test**: Mozna przetestowac uruchamiajac analize, restartujac aplikacje i weryfikujac ze wyniki sa nadal dostepne.

**Acceptance Scenarios**:

1. **Given** analiza zostala zakonczona, **When** uzytkownik wraca na dashboard, **Then** poprzednie wyniki sa dostepne z bazy danych
2. **Given** uzytkownik uruchamia nowa analize tego samego repozytorium, **When** analiza sie zakonczy, **Then** nowe wyniki sa zapisane obok poprzednich (nie nadpisuja)

---

### Edge Cases

- Co jesli GitHub API zwraca rate limit (403)? System powinien wyswietlic komunikat i zaproponowac zmniejszenie limitu PR.
- Co jesli PR nie ma zadnych zmienionych plikow (np. empty merge)? System powinien wykluczyc go z analizy.
- Co jesli token GitHub nie ma uprawnien do danego repozytorium? System powinien wyswietlic czytelny komunikat bledu.
- Co jesli repozytorium ma tysiace PR? System powinien respektowac limit (domyslnie 100) i paginowac.
- Co jesli Claude CLI nie jest zainstalowane na maszynie? Adapter powinien wykryc to przy starcie i wyswietlic ostrzezenie, fallback na NoOp.
- Co jesli wszystkie PR zostana wykluczone przez reguly? Dashboard powinien wyswietlic komunikat "Brak PR spelniajacych kryteria analizy".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST pobierac Pull Requesty z GitHub API na podstawie project slug, zakresu dat i limitu
- **FR-002**: System MUST oceniac kazdy PR silnikiem regul (exclude/boost/penalize) i przypisywac score (0.0-1.0) oraz verdict
- **FR-003**: System MUST udostepniac REST API do uruchamiania analiz i pobierania wynikow
- **FR-004**: System MUST wyswietlac dashboard z formularzem analizy, tabela wynikow i podsumowaniem
- **FR-005**: System MUST wyswietlac szczegoly PR ze score breakdown (ktore reguly zadzialaly)
- **FR-006**: System MUST obslugiwac konfiguracje regul przez application.yml
- **FR-007**: System MUST obslugiwac wymienne adaptery MergeRequestProvider (GitHub na start) bez zmian w logice domenowej
- **FR-008**: System MUST obslugiwac wymienne adaptery LlmAnalyzer (Claude CLI i NoOp na start) bez zmian w logice domenowej
- **FR-009**: System MUST zapisywac wyniki analiz w bazie danych (H2 w dev)
- **FR-010**: System MUST obslugiwac bledy API (rate limit, auth, network) z czytelnymi komunikatami
- **FR-011**: System MUST kolorowac wyniki w tabeli wg verdict (zielony/zolty/czerwony)
- **FR-012**: System MUST automatycznie paginowac wyniki z GitHub API
- **FR-013**: System MUST miec testy BDD (Cucumber/Gherkin) pokrywajace acceptance scenarios
- **FR-014**: System MUST miec testy jednostkowe dla domeny (scoring, rules)

### Key Entities

- **MergeRequest**: Zunifikowany model PR/MR — tytul, opis, autor, branch, labels, zmienione pliki, diff stats, provider, URL
- **AnalysisResult**: Wynik analizy PR — score (0-1), verdict (AUTOMATABLE/MAYBE/NOT_SUITABLE), lista powodow, matched rules, opcjonalny komentarz LLM
- **AnalysisReport**: Raport zbiorczy — lista wynikow, podsumowanie (counts per verdict), metadata (repo, daty, provider)
- **Rule**: Regula oceny — typ (exclude/boost/penalize), warunek, waga, opis
- **FetchCriteria**: Kryteria pobierania PR — project slug, branch, zakres dat, limit, state

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uzytkownik moze uruchomic analize repozytorium GitHub i zobaczyc wyniki w przegladarce w mniej niz 2 minuty od otwarcia aplikacji
- **SC-002**: System poprawnie klasyfikuje PR na podstawie regul — PR z labelami exclude dostaja verdict NOT_SUITABLE w 100% przypadkow
- **SC-003**: Score breakdown na stronie szczegulow PR zawiera co najmniej nazwe reguly, typ (boost/penalize/exclude) i wartosc wagi
- **SC-004**: Testy BDD pokrywaja wszystkie acceptance scenarios z user stories (min. 10 scenariuszy .feature)
- **SC-005**: Testy jednostkowe pokrywaja silnik regul i scoring (min. 15 test cases)
- **SC-006**: Aplikacja startuje i jest dostepna pod adresem localhost:8083 w mniej niz 30 sekund
