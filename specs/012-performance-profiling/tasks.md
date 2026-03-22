# Tasks: 012 — Performance Profiling

**Input**: Design documents from `/specs/012-performance-profiling/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Unit test DiagnosticsController (BDD pominiete — uzasadnienie w plan.md Gate III).

**Organization**: Tasks pogrupowane wg user stories ze spec.md (US1–US6).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Moze byc wykonane rownolegle (rozne pliki, brak zaleznosci)
- **[Story]**: Ktora user story (US1–US6)

---

## Phase 1: Setup

**Purpose**: Zaleznosci Maven, konfiguracja Actuator/Hibernate, katalog raportow

- [x] T001 Dodac spring-boot-starter-actuator i micrometer-registry-prometheus do `backend/pom.xml`
- [x] T002 Skonfigurowac Actuator endpoints w `backend/src/main/resources/application.yml` (management.endpoints.web.exposure.include: health,metrics,prometheus)
- [x] T003 Wlaczyc Hibernate Statistics w profilu dev w `backend/src/main/resources/application.yml` (spring.jpa.properties.hibernate.generate_statistics: true)
- [x] T004 [P] Dodac `reports/` do `.gitignore`
- [x] T005 [P] Utworzyc katalog `scripts/` i `reports/flamegraphs/` (gitkeep)

**Checkpoint**: `mvn clean install` przechodzi, `/actuator/health` odpowiada po uruchomieniu serwera

---

## Phase 2: Foundational — DiagnosticsController

**Purpose**: Endpoint REST do odczytu/resetu statystyk Hibernate — blokuje US4 (SQL) i US6 (raport)

**⚠️ CRITICAL**: Skrypt profile.sh potrzebuje tego endpointu do zbierania metryk SQL

- [x] T006 Stworzyc `DiagnosticsController.java` w `backend/src/main/java/com/mranalizer/adapter/in/rest/DiagnosticsController.java` — `@Profile("dev")`, `@RestController`, `@RequestMapping("/api/diagnostics")`
- [x] T007 Implementowac `GET /api/diagnostics/sql-stats` — odczyt Hibernate Statistics z EntityManagerFactory (queryExecutionCount, maxTime, entityLoadCount itp.)
- [x] T008 Implementowac `POST /api/diagnostics/sql-stats/reset` — reset statystyk Hibernate
- [x] T009 Stworzyc `SqlStatsResponse.java` record w `backend/src/main/java/com/mranalizer/adapter/in/rest/dto/SqlStatsResponse.java`
- [x] T010 Unit test `DiagnosticsControllerTest.java` w `backend/src/test/java/com/mranalizer/adapter/in/rest/DiagnosticsControllerTest.java` — mock EntityManagerFactory, sprawdzic GET i POST

**Checkpoint**: `mvn test` przechodzi, `curl localhost:8083/api/diagnostics/sql-stats` zwraca JSON (profil dev)

---

## Phase 3: User Story 1 — Komenda `/profile` w Claude (Priority: P1) 🎯 MVP

**Goal**: Komenda `/profile` uruchamia profilowanie na dzialajacym serwerze

**Independent Test**: Uruchom `/profile` w Claude — powinno sprawdzic health, wykryc PID, wyswietlic podsumowanie

### Implementation for User Story 1

- [x] T011 [US1] Stworzyc `.claude/commands/profile.md` — prompt komenda: instrukcje dla Claude (sprawdz serwer, uruchom skrypt, przeanalizuj wyniki, wygeneruj rekomendacje)
- [x] T012 [US1] W prompcie obsluzyc parametry: `--duration` (default 30s), `--with-load`, `--type` (cpu|memory|full, default full)
- [x] T013 [US1] W prompcie dodac obsluge bledu: serwer nie dziala → informuj i zaproponuj `cd backend && mvn spring-boot:run`
- [x] T014 [US1] W prompcie dodac obsluge bledu: async-profiler nie zainstalowany → instrukcja instalacji z quickstart.md

**Checkpoint**: `/profile` w Claude wykrywa stan serwera i reaguje odpowiednio

---

## Phase 4: User Story 2 — CPU i alokacje (async-profiler) (Priority: P2)

**Goal**: Flame graphs CPU i alokacji z async-profiler

**Independent Test**: Uruchom `scripts/profile.sh --type cpu` — powinno wygenerowac flame graph HTML

### Implementation for User Story 2

- [x] T015 [US2] Stworzyc `scripts/profile.sh` — szkielet skryptu z parsowaniem argumentow (getopts: --duration, --with-load, --type, --load-iterations, --load-concurrency)
- [x] T016 [US2] Implementowac health check w `scripts/profile.sh` — `curl -sf localhost:8083/actuator/health`, exit jesli serwer nie dziala
- [x] T017 [US2] Implementowac PID detection w `scripts/profile.sh` — `pgrep -f 'MrAnalizerApplication\|mr-analizer'`
- [x] T018 [US2] Implementowac async-profiler attach w `scripts/profile.sh` — CPU profiling: `asprof -d $DURATION -f reports/flamegraphs/cpu-$TS.html $PID`
- [x] T019 [US2] Implementowac alloc profiling w `scripts/profile.sh` — `asprof -d $DURATION -e alloc -f reports/flamegraphs/alloc-$TS.html $PID`
- [x] T020 [US2] Dodac fallback w `scripts/profile.sh` — jesli async-profiler nie znaleziony, wyswietl instrukcje instalacji i kontynuuj bez flame graphs

**Checkpoint**: `bash scripts/profile.sh --type cpu --duration 10` generuje `reports/flamegraphs/cpu-*.html`

---

## Phase 5: User Story 3 — Metryki HTTP i JVM (Actuator) (Priority: P3)

**Goal**: Odczyt czasow odpowiedzi endpointow i stanu JVM z Actuator

**Independent Test**: `curl localhost:8083/actuator/metrics/http.server.requests` zwraca metryki po kilku requestach

### Implementation for User Story 3

- [x] T021 [US3] Dodac do `scripts/profile.sh` funkcje zbierajaca metryki HTTP — `curl /actuator/metrics/http.server.requests` z parsowaniem JSON (jq)
- [x] T022 [US3] Dodac do `scripts/profile.sh` funkcje zbierajaca metryki JVM — heap (`jvm.memory.used`), GC (`jvm.gc.pause`), threads (`jvm.threads.live`)
- [x] T023 [US3] Formatowac metryki HTTP jako tabele Markdown (endpoint, count, totalTime, max) w raporcie

**Checkpoint**: Sekcje "HTTP Latency" i "Memory & GC" w wygenerowanym raporcie zawieraja dane

---

## Phase 6: User Story 4 — Metryki SQL / Hibernate (Priority: P4)

**Goal**: Liczba zapytan SQL, slow queries, N+1 detection w raporcie

**Independent Test**: Po load testu `curl localhost:8083/api/diagnostics/sql-stats` zwraca niezerowe wartosci

### Implementation for User Story 4

- [x] T024 [US4] Dodac do `scripts/profile.sh` reset SQL stats przed profilowaniem — `curl -X POST /api/diagnostics/sql-stats/reset`
- [x] T025 [US4] Dodac do `scripts/profile.sh` zbieranie SQL stats po profilowaniu — `curl /api/diagnostics/sql-stats` z parsowaniem JSON
- [x] T026 [US4] Formatowac metryki SQL w raporcie — total queries, slowest query, entity load count, flaga N+1 (>10 queries per request)

**Checkpoint**: Sekcja "SQL Analysis" w raporcie zawiera dane z Hibernate Statistics

---

## Phase 7: User Story 5 — Generowanie obciazenia testowego (Priority: P5)

**Goal**: Flaga `--with-load` generuje realistyczne obciazenie na endpointach REST

**Independent Test**: `bash scripts/profile.sh --with-load --load-iterations 5` wykonuje requesty i w raporcie widac niezerowe count HTTP

### Implementation for User Story 5

- [x] T027 [US5] Dodac do `scripts/profile.sh` funkcje load generation — sekwencja curl: POST /api/browse, GET /api/analysis, GET /api/repos
- [x] T028 [US5] Implementowac concurrency w load generation — `xargs -P $CONCURRENCY` lub `parallel`
- [x] T029 [US5] Parametryzowac load: `--load-iterations` (default 10), `--load-concurrency` (default 3)
- [x] T030 [US5] Dodac mock/testowe dane do load generation — prosty JSON body dla POST /api/browse (projectSlug testowy)

**Checkpoint**: `--with-load` generuje requesty, Actuator metryki pokazuja niezerowe count

---

## Phase 8: User Story 6 — Raport z rekomendacjami (Priority: P6)

**Goal**: Raport Markdown z sekcjami Summary, CPU, Memory, HTTP, SQL, Recommendations

**Independent Test**: Po pelnym profilowaniu plik `reports/profile-*.md` zawiera wszystkie sekcje

### Implementation for User Story 6

- [x] T031 [US6] Dodac do `scripts/profile.sh` generowanie raportu Markdown — template z sekcjami: Summary, CPU Hotspots, Memory & GC, HTTP Latency, SQL Analysis
- [x] T032 [US6] Wypelniac sekcje raportu danymi zebranymi w poprzednich fazach (metryki HTTP, JVM, SQL, linki do flame graphs)
- [x] T033 [US6] W `.claude/commands/profile.md` dodac instrukcje dla Claude: po uruchomieniu skryptu przeczytaj raport, dodaj sekcje "Recommendations" z 3-5 konkretnymi rekomendacjami (severity, problem, impact, fix)
- [x] T034 [US6] Kazda rekomendacja Claude powinna zawierac: `[HIGH|MEDIUM|LOW]`, opis problemu, wplyw, sugerowana poprawka z odniesieniem do kodu

**Checkpoint**: Pelny flow `/profile --with-load` generuje kompletny raport z rekomendacjami Claude

---

## Phase 9: Polish & Finalizacja

**Purpose**: Dokumentacja, aktualizacja CLAUDE.md, walidacja

- [x] T035 [P] Zaktualizowac `CLAUDE.md` — dodac sekcje: komenda `/profile`, endpoint `/api/diagnostics/sql-stats`, katalog `reports/`
- [x] T036 [P] Zaktualizowac `SPEC.md` — dodac wpis o 012-performance-profiling w "Zrealizowane features"
- [x] T037 Smoke test calego flow: uruchom serwer → `/profile --with-load --duration 15` → sprawdz raport + flame graphs

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Brak zaleznosci — start natychmiast
- **Phase 2 (DiagnosticsController)**: Zalezy od Phase 1 (actuator w pom.xml)
- **Phase 3 (US1 — komenda /profile)**: Moze byc rownolegla z Phase 2, ale pelna funkcjonalnosc wymaga skryptu (Phase 4+)
- **Phase 4 (US2 — async-profiler)**: Zalezy od Phase 1 (katalogi)
- **Phase 5 (US3 — Actuator metryki)**: Zalezy od Phase 1 (actuator config)
- **Phase 6 (US4 — SQL stats)**: Zalezy od Phase 2 (DiagnosticsController)
- **Phase 7 (US5 — load gen)**: Zalezy od Phase 4 (skrypt istnieje)
- **Phase 8 (US6 — raport)**: Zalezy od Phase 4-7 (dane do raportu)
- **Phase 9 (Polish)**: Zalezy od wszystkich poprzednich

### User Story Dependencies

```
Phase 1 (Setup)
  │
  ├──→ Phase 2 (DiagnosticsController)
  │       │
  │       └──→ Phase 6 (US4 — SQL stats)
  │
  ├──→ Phase 4 (US2 — async-profiler) ←── szkielet skryptu
  │       │
  │       ├──→ Phase 5 (US3 — Actuator metryki)  [P]
  │       ├──→ Phase 7 (US5 — load gen)
  │       └──→ Phase 8 (US6 — raport) ←── wymaga Phase 5, 6, 7
  │
  └──→ Phase 3 (US1 — komenda /profile) ←── niezalezna, ale pelna po Phase 8
```

### Parallel Opportunities

- **T004 + T005**: gitignore + katalogi — rozne pliki
- **T035 + T036**: CLAUDE.md + SPEC.md — rozne pliki
- **Phase 3 (komenda) || Phase 4 (skrypt)**: moga byc rozwijane rownolegle
- **Phase 5 (HTTP) || Phase 6 (SQL)**: niezalezne sekcje skryptu, moga byc rownolegle

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1: Setup (T001–T005)
2. Phase 4: Skrypt z async-profiler (T015–T020)
3. Phase 3: Komenda `/profile` (T011–T014)
4. **STOP**: Masz dzialajace `/profile` z flame graphs CPU/alloc
5. Dodawaj US3–US6 inkrementalnie

### Full Delivery

1. Phase 1 → Phase 2 → Phase 4 → Phase 3
2. Phase 5 + Phase 6 (rownolegle)
3. Phase 7 → Phase 8
4. Phase 9 (polish)

---

## Notes

- Skrypt `profile.sh` rosnie inkrementalnie — kazda US dodaje funkcje do tego samego pliku
- async-profiler musi byc zainstalowany osobno (instrukcja w quickstart.md)
- Raporty w `reports/` sa gitignore — artefakty deweloperskie
- Komenda `/profile` deleguje do skryptu bash, Claude dodaje wartosc przez analize i rekomendacje
- `jq` wymagane do parsowania JSON z Actuator — sprawdzic dostepnosc w skrypcie
