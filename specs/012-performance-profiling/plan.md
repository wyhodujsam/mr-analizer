# Implementation Plan: 012 — Performance Profiling

**Branch**: `012-performance-profiling` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-performance-profiling/spec.md`

## Summary

Dodanie profilowania wydajnosci aplikacji mr_analizer uruchamianego komenda `/profile` w Claude. Kompozycja 3 narzedzi: async-profiler (CPU/alloc flame graphs), Spring Boot Actuator (metryki HTTP/JVM), Hibernate Statistics (metryki SQL). Wynik: raport Markdown z flame graphs i rekomendacjami Claude.

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.2.5
**Primary Dependencies**: spring-boot-starter-actuator (nowa), async-profiler v3.0 (zewnetrzne CLI)
**Storage**: Brak nowych tabel — raporty jako pliki Markdown w `reports/`
**Testing**: JUnit 5 — test DiagnosticsController (unit)
**Target Platform**: Linux (x64/ARM64)
**Project Type**: Web service (backend Spring Boot + React SPA)
**Performance Goals**: N/A — to jest narzedzie DO mierzenia wydajnosci
**Constraints**: Musi dzialac bez GUI (CLI/mobile workflow), zero-overhead profilowanie
**Scale/Scope**: Dev-only narzedzie, 1 developer

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Hexagonal Architecture | PASS | DiagnosticsController to adapter/in/rest — nie dotyka domeny. Actuator to zewnetrzna biblioteka Spring (adapter config). |
| II. Provider Abstraction | PASS | Nie dodajemy nowego providera. Hibernate Stats odczytywane z EntityManagerFactory (infrastruktura). |
| III. BDD Testing | PASS* | *Uzasadnienie ponizej. Feature to narzedzie deweloperskie — BDD scenariusze nie maja sensu. Unit test DiagnosticsController pokryje endpoint SQL stats. |
| IV. SDD Workflow | PASS | Pelny flow: spec → plan → tasks → implement |
| V. Simplicity (YAGNI) | PASS | Minimalne zmiany w backendzie (1 controller, 2 zaleznosci). Logika w bash script. Brak nowych abstrakcji domenowych. |

### Gate III justification (BDD)

Profilowanie to narzedzie deweloperskie — nie feature uzytkownika koncowego. Scenariusze Cucumber opisuja zachowanie z perspektywy usera biznesowego, a tu "user" to developer uruchamiajacy `/profile`. BDD scenariusze dla skryptu bash bylyby sztuczne. Zamiast tego:
- Unit test `DiagnosticsControllerTest` — sprawdza odpowiedz SQL stats
- Smoke test skryptu `profile.sh` — reczny

## Project Structure

### Documentation (this feature)

```text
specs/012-performance-profiling/
├── spec.md              # Specyfikacja feature'a
├── plan.md              # Ten plik
├── research.md          # Badania narzedzi i podejsc
├── data-model.md        # Struktury danych (DTO, raport)
├── quickstart.md        # Instrukcja uzycia
└── tasks.md             # Breakdown na taski (po /speckit.tasks)
```

### Source Code (repository root)

```text
backend/
├── pom.xml                                          # +2 dependencies (actuator, micrometer-prometheus)
└── src/
    ├── main/
    │   ├── java/com/mranalizer/
    │   │   └── adapter/
    │   │       └── in/rest/
    │   │           └── DiagnosticsController.java   # NOWY — GET/POST /api/diagnostics/sql-stats
    │   └── resources/
    │       └── application.yml                      # +actuator config, +hibernate stats
    └── test/
        └── java/com/mranalizer/
            └── adapter/in/rest/
                └── DiagnosticsControllerTest.java   # NOWY — unit test

scripts/
└── profile.sh                                       # NOWY — glowny skrypt profilujacy

.claude/commands/
└── profile.md                                       # NOWY — komenda Claude /profile

reports/                                             # NOWY katalog (gitignore)
└── flamegraphs/                                     # flame graphs HTML

.gitignore                                           # +reports/
```

## Design Decisions

### D1: Skrypt bash vs Java profiling endpoint

**Decyzja**: Logika profilowania w `scripts/profile.sh`, nie w Java.

**Uzasadnienie**:
- async-profiler to zewnetrzne narzedzie CLI — naturalnie wola sie z basha
- Actuator metryki odczytuje sie przez HTTP (curl)
- Bash laczy te 3 zrodla danych w jeden flow
- Nie zasmiecamy domeny/aplikacji kodem diagnostycznym

### D2: Raport jako Markdown, nie encja JPA

**Decyzja**: Raporty profilowania to pliki `.md` w `reports/`, nie rekordy w H2.

**Uzasadnienie**:
- Raporty profilowania to artefakty deweloperskie, nie dane biznesowe
- Flame graphs to pliki HTML — nie pasuja do relacyjnej bazy
- Prostota — brak migracji, encji, repository
- `reports/` w `.gitignore`

### D3: DiagnosticsController tylko w profilu dev

**Decyzja**: `@Profile("dev")` na DiagnosticsController.

**Uzasadnienie**:
- Hibernate Statistics nie powinny byc dostepne publicznie
- Profil dev juz uzywany w projekcie (H2 console)

### D4: Komenda Claude jako orkiestrator

**Decyzja**: `/profile` komenda Claude uruchamia skrypt i analizuje wyniki.

**Uzasadnienie**:
- Claude interpretuje dane i generuje rekomendacje — wartosc dodana vs surowe liczby
- Komenda reaguje na kontekst (serwer nie dziala → zaproponuj uruchomienie)
- Developer nie musi pamietac parametrow async-profiler

## Component Flow

```
Developer
  │
  ├─ /profile [--duration 30] [--with-load] [--type full]
  │
  ▼
Claude (profile.md prompt)
  │
  ├─ 1. Health check: curl localhost:8083/actuator/health
  │     └─ Serwer nie dziala? → informuj, zaproponuj uruchomienie
  │
  ├─ 2. Reset metryk:
  │     ├─ POST /api/diagnostics/sql-stats/reset
  │     └─ (Actuator resetuje sie per request)
  │
  ├─ 3. async-profiler attach:
  │     ├─ PID = pgrep -f MrAnalizerApplication
  │     ├─ asprof -d {duration} -f reports/flamegraphs/cpu-{ts}.html $PID
  │     └─ asprof -d {duration} -e alloc -f reports/flamegraphs/alloc-{ts}.html $PID
  │
  ├─ 4. [--with-load] Generowanie obciazenia:
  │     ├─ curl POST /api/browse (mock data)
  │     ├─ curl POST /api/analysis
  │     ├─ curl GET /api/analysis
  │     ├─ curl GET /api/analysis/{id}
  │     └─ Powtorz N razy, M concurrent (xargs -P)
  │
  ├─ 5. Zbierz metryki:
  │     ├─ GET /actuator/metrics/http.server.requests
  │     ├─ GET /actuator/metrics/jvm.memory.used
  │     ├─ GET /actuator/metrics/jvm.gc.pause
  │     └─ GET /api/diagnostics/sql-stats
  │
  ├─ 6. Generuj raport:
  │     ├─ Parse metryki → tabele Markdown
  │     ├─ Link flame graphs
  │     └─ Zapisz reports/profile-{timestamp}.md
  │
  └─ 7. Analiza + rekomendacje:
        └─ Claude analizuje metryki, dodaje 3-5 rekomendacji do raportu
```

## Implementation Phases

### Phase 1: Backend — Actuator + Hibernate Stats + DiagnosticsController

1. Dodac zaleznosci do `pom.xml` (actuator, micrometer-prometheus)
2. Skonfigurowac Actuator w `application.yml` (expose health, metrics)
3. Wlaczyc Hibernate Statistics w profilu dev
4. Stworzyc `DiagnosticsController` z endpointami SQL stats
5. Unit test `DiagnosticsControllerTest`

### Phase 2: Skrypt profilujacy `scripts/profile.sh`

1. Stworzyc `scripts/profile.sh` z parametrami CLI
2. Health check + PID detection
3. async-profiler attach (CPU + alloc)
4. Load generation (curl loop z xargs -P)
5. Zbieranie metryk (curl Actuator + diagnostics)
6. Generowanie raportu Markdown (template z placeholderami)

### Phase 3: Komenda Claude `/profile`

1. Stworzyc `.claude/commands/profile.md`
2. Prompt: uruchom skrypt, przeanalizuj wyniki, dodaj rekomendacje
3. Obsluga bledow (serwer nie dziala, brak async-profiler)

### Phase 4: Finalizacja

1. Dodac `reports/` do `.gitignore`
2. Aktualizacja CLAUDE.md (nowa komenda, nowe endpointy)
3. Smoke test calego flow

## Complexity Tracking

Brak naruszen constitution — feature jest prosty i zgodny ze wszystkimi zasadami.
