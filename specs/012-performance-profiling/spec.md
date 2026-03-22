# 012 — Performance Profiling (analiza wydajnosci)

## Cel

Dodanie mozliwosci profilowania wydajnosci aplikacji mr_analizer — uruchamiane na zadanie komenda Claude (`/profile`). Profilowanie zbiera metryki CPU, pamieci, czasow odpowiedzi HTTP i zapytan SQL, a nastepnie generuje raport z rekomendacjami optymalizacji.

## Kontekst i motywacja

Aplikacja mr_analizer wykonuje operacje potencjalnie kosztowne: wywolania GitHub API, analiza LLM (Claude CLI), scoring wielu MR, operacje JPA. Brakuje narzedzia do systematycznej analizy wydajnosci — obecnie problemy wydajnosciowe sa wykrywane ad hoc (np. spec 009). Potrzebny jest mechanizm, ktory:

- uruchomi profilowanie na dzialajacym serwerze
- zbierze metryki w kontrolowany sposob (z obciazeniem lub bez)
- wygeneruje czytelny raport z bottleneckami
- bedzie mozliwy do uruchomienia jednym poleceniem Claude

## Wybor narzedzia profilujacego

### Opcje rozwazone

| Narzedzie | Zalety | Wady | Verdict |
|-----------|--------|------|---------|
| **JProfiler** | Najlepszy GUI, szczegolowe CPU/memory trees | Komercyjna licencja, wymaga agenta JVM, ciezki GUI (nie pasuje do workflow mobilnego/CLI) | Odrzucone |
| **async-profiler** | Lekki, CLI-native, flame graphs, zero overhead sampling, open source | Tylko CPU/alloc (brak HTTP/SQL z pudla) | **Wybrany — CPU/alloc** |
| **Spring Boot Actuator + Micrometer** | Wbudowany w Spring, metryki HTTP/JVM/hikari, eksport do Prometheus | Nie profiluje CPU per-metoda, wymaga konfiguracji endpointow | **Wybrany — metryki HTTP/JVM** |
| **Hibernate Statistics** | Szczegolowe metryki SQL (query count, slow queries, cache) | Trzeba wlaczyc w konfiguracji | **Wybrany — metryki SQL** |
| **VisualVM** | Darmowy, GUI | Wymaga GUI — nie pasuje do CLI/mobile workflow | Odrzucone |

### Decyzja: kompozycja 3 narzedzi

Zamiast jednego ciezkiego profilera — lekka kompozycja:

1. **async-profiler** — CPU flame graph + allocation profiling (zero-overhead, CLI-native)
2. **Spring Boot Actuator + Micrometer** — metryki HTTP (czasy odpowiedzi, throughput), JVM (heap, GC, threads)
3. **Hibernate Statistics** — liczba zapytan SQL, slow queries, N+1 detection

## User Stories

### US-1: Komenda `/profile` w Claude

**Jako** developer
**Chce** uruchomic profilowanie aplikacji jednym poleceniem `/profile`
**Abym** mogl szybko zbadac wydajnosc bez recznej konfiguracji

**AC:**
- AC-1.1: Komenda `/profile` uruchamia profilowanie na dzialajacym serwerze (port 8083)
- AC-1.2: Komenda akceptuje opcjonalne parametry: `--duration` (domyslnie 30s), `--with-load` (generowanie obciazenia), `--type` (cpu|memory|full, domyslnie full)
- AC-1.3: Po zakonczeniu profilowania wyswietla podsumowanie i sciezke do raportu
- AC-1.4: Jesli serwer nie dziala, komenda informuje o tym i proponuje uruchomienie

### US-2: Zbieranie metryk CPU i alokacji

**Jako** developer
**Chce** zobaczyc ktore metody zuzyja najwiecej CPU i alokuja najwiecej pamieci
**Abym** wiedzial gdzie optymalizowac

**AC:**
- AC-2.1: async-profiler generuje flame graph (SVG) z top hotspotami CPU
- AC-2.2: async-profiler generuje alloc flame graph (SVG) z top alokacjami
- AC-2.3: Raport zawiera top-10 hotspotow z procentowym udzialem CPU
- AC-2.4: Jesli async-profiler nie jest zainstalowany, komenda informuje jak go zainstalowac

### US-3: Metryki HTTP i JVM (Actuator)

**Jako** developer
**Chce** widziec czasy odpowiedzi endpointow REST i stan JVM
**Abym** zidentyfikowal wolne endpointy i problemy z pamiecia

**AC:**
- AC-3.1: Raport zawiera czasy p50/p95/p99 kazdego endpointu REST
- AC-3.2: Raport zawiera metryki JVM: heap used/max, GC pauses, active threads
- AC-3.3: Jesli Actuator nie jest wlaczony, komenda wlacza go automatycznie (wymaga restartu serwera)

### US-4: Metryki SQL / Hibernate

**Jako** developer
**Chce** widziec ile zapytan SQL generuje kazda operacja
**Abym** wylapal problemy N+1 i zbedne query

**AC:**
- AC-4.1: Raport zawiera calkowita liczbe zapytan SQL per endpoint
- AC-4.2: Raport flaguje potencjalne N+1 (>10 zapytan na jedno wywolanie REST)
- AC-4.3: Raport zawiera najwolniejsze zapytania SQL (>100ms)

### US-5: Generowanie obciazenia testowego

**Jako** developer
**Chce** uruchomic profilowanie z realistycznym obciazeniem
**Abym** widzial wydajnosc pod presja a nie na idle

**AC:**
- AC-5.1: Flaga `--with-load` uruchamia sekwencje typowych operacji (browse, analyze, list reports)
- AC-5.2: Obciazenie uzywa testowych danych (mock provider, bez realnych wywolan GitHub/LLM)
- AC-5.3: Load generation konfigurowalny: liczba powtorzen (domyslnie 10), concurrency (domyslnie 3)

### US-6: Raport z rekomendacjami

**Jako** developer
**Chce** dostac czytelny raport z konkretnymi rekomendacjami
**Abym** wiedzial CO poprawic, nie tylko GDZIE jest wolno

**AC:**
- AC-6.1: Raport tekstowy (Markdown) zapisany w `reports/profile-{timestamp}.md`
- AC-6.2: Flame graphs (SVG) w `reports/flamegraphs/`
- AC-6.3: Raport zawiera sekcje: Summary, CPU Hotspots, Memory, HTTP Latency, SQL Analysis, Recommendations
- AC-6.4: Claude analizuje zebrane metryki i generuje 3-5 konkretnych rekomendacji optymalizacji
- AC-6.5: Kazda rekomendacja zawiera: opis problemu, wplyw (high/medium/low), sugerowana poprawka

## Architektura rozwiazania

### Komenda Claude (`/profile`)

Plik: `.claude/commands/profile.md`

```
Komenda uruchamia profilowanie wydajnosci aplikacji mr_analizer.
```

Komenda:
1. Sprawdza czy serwer dziala (curl health check na 8083)
2. Sprawdza dostepnosc async-profiler
3. Odpala profilowanie (async-profiler attach do PID)
4. Opcjonalnie generuje obciazenie (curl do endpointow REST)
5. Zbiera metryki z Actuator i Hibernate Stats
6. Generuje raport Markdown + flame graphs SVG
7. Analizuje wyniki i dopisuje rekomendacje

### Zmiany w backendzie

#### 1. Spring Boot Actuator (nowa zaleznosc)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### 2. Konfiguracja Actuator (application.yml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: mr-analizer
```

#### 3. Hibernate Statistics (application.yml)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
    # Nowy endpoint do pobrania statystyk Hibernate
logging:
  level:
    org.hibernate.stat: DEBUG
```

#### 4. Endpoint metryk SQL (nowy)

```java
// adapter/in/rest/DiagnosticsController.java
@RestController
@RequestMapping("/api/diagnostics")
@Profile("dev")
public class DiagnosticsController {

    @GetMapping("/sql-stats")
    public SqlStatsResponse getSqlStats() { ... }

    @PostMapping("/sql-stats/reset")
    public void resetSqlStats() { ... }
}
```

### async-profiler

Instalacja (jednorazowa):
```bash
# Linux x64/ARM
wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
tar xzf async-profiler-3.0-linux-x64.tar.gz -C ~/tools/
```

Uzycie z komendy `/profile`:
```bash
# Znajdz PID serwera
PID=$(pgrep -f 'mr_analizer\|mranalizer\|MrAnalizerApplication')

# CPU profiling (30s) → flame graph SVG
~/tools/async-profiler/bin/asprof -d 30 -f reports/flamegraphs/cpu-$(date +%s).html $PID

# Allocation profiling → alloc flame graph
~/tools/async-profiler/bin/asprof -d 30 -e alloc -f reports/flamegraphs/alloc-$(date +%s).html $PID
```

### Struktura raportu

```markdown
# Performance Profile Report — 2026-03-22 14:30

## Summary
- Duration: 30s
- Load: yes (10 iterations, 3 concurrent)
- Server PID: 12345
- JVM: OpenJDK 17.0.x

## CPU Hotspots (async-profiler)
| # | Method | CPU % | Samples |
|---|--------|-------|---------|
| 1 | ScoringEngine.evaluate() | 12.3% | 1840 |
| 2 | GitHubClient.fetchPage() | 8.7% | 1305 |
| ...

Flame graph: [cpu-1711234567.html](flamegraphs/cpu-1711234567.html)

## Memory & GC
- Heap used: 128MB / 512MB (25%)
- GC pauses (total): 45ms (3 pauses)
- Top allocations: see [alloc flame graph](flamegraphs/alloc-1711234567.html)

## HTTP Latency (Actuator)
| Endpoint | p50 | p95 | p99 | Count |
|----------|-----|-----|-----|-------|
| POST /api/analysis | 1.2s | 3.4s | 5.1s | 30 |
| POST /api/browse | 0.8s | 1.5s | 2.1s | 30 |
| GET /api/analysis | 12ms | 45ms | 78ms | 30 |

## SQL Analysis (Hibernate Stats)
- Total queries: 342
- Queries per /api/analysis call: 18 ⚠️ (potential N+1)
- Slowest query: 145ms (AnalysisResultEntity findByReportId)

## Recommendations

### 1. [HIGH] N+1 w AnalysisResult → AnalysisReport
**Problem:** 18 zapytan SQL na jedno wywolanie /api/analysis/{id}
**Wplyw:** Czas odpowiedzi rosnie liniowo z liczba wynikow
**Fix:** Dodac `@EntityGraph` lub `JOIN FETCH` w repository query

### 2. [MEDIUM] ScoringEngine.evaluate() — hot loop
**Problem:** 12.3% CPU w petli scoringu regul
**Wplyw:** Przy duzej liczbie MR scoring jest bottleneckiem
**Fix:** Cache wynikow regul per MR (reguly sa deterministic)

### 3. [LOW] Zbedne alokacje String w GitHubMapper
**Problem:** Czeste tworzenie tymczasowych Stringow w mapowaniu DTO
**Fix:** StringBuilder lub String.join zamiast konkatenacji
```

## Ograniczenia i ryzyka

| Ryzyko | Mitygacja |
|--------|-----------|
| async-profiler wymaga `perf_events` na Linux | Fallback: `-e itimer` zamiast `-e cpu` |
| Actuator otwiera dodatkowe endpointy | Tylko profil `dev`, nie na produkcji |
| Profilowanie pod obciazeniem moze byc niereprezentatywne (mock provider) | Dokumentacja w raporcie ze to synthetic load |
| async-profiler moze nie byc zainstalowany | Komenda sprawdza i instruuje instalacje |

## Szacowany zakres zmian

### Backend
- `pom.xml` — 2 nowe zaleznosci (actuator, micrometer-prometheus)
- `application.yml` — konfiguracja actuator + hibernate stats
- `DiagnosticsController.java` — nowy controller (tylko profil dev)
- `SqlStatsResponse.java` — DTO dla statystyk SQL

### Komenda Claude
- `.claude/commands/profile.md` — prompt komendy /profile

### Pliki pomocnicze
- `scripts/profile.sh` — skrypt profilujacy (async-profiler + curl Actuator + raport)
- `reports/` — katalog na raporty (gitignore)

## Poza zakresem (out of scope)

- Profilowanie frontendu (React) — osobny feature
- Continuous profiling (np. Pyroscope) — overkill dla dev-only narzedzia
- Wizualizacja metryk w GUI (Grafana) — CLI/mobile workflow, raport MD wystarczy
- Profilowanie na produkcji — to jest narzedzie deweloperskie
- Automatyczne naprawianie problemow wydajnosciowych — raport + rekomendacje, fix recznie
