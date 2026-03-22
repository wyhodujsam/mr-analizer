# Research: 012 — Performance Profiling

## 1. async-profiler — integracja z Java 17 / Spring Boot

### Decision
Uzyc async-profiler v3.0+ jako glownego profilera CPU/alloc. Attach do dzialajacego procesu JVM przez `asprof` CLI.

### Rationale
- Zero-overhead sampling (nie spowalnia aplikacji)
- CLI-native — pasuje do workflow mobilnego/Claude
- Generuje flame graphs (HTML) bez dodatkowych narzedzi
- Open source (Apache 2.0)
- Wspiera Java 17 (od v2.0)

### Alternatives considered
- **JProfiler**: Najlepszy GUI profiler, ale wymaga licencji ($499) i GUI — nie pasuje do CLI workflow
- **VisualVM**: Darmowy, ale wymaga GUI (X11/VNC)
- **JFR (Java Flight Recorder)**: Wbudowany w JDK, ale generuje binarny .jfr — wymaga JDK Mission Control do analizy (GUI)
- **YourKit**: Komercyjny, GUI-centric

### Szczegoly techniczne
- Instalacja: `wget` + `tar` do `~/tools/async-profiler/`
- Attach: `asprof -d <seconds> -f <output> <pid>`
- Output: HTML flame graph (interaktywny, otwarcie w przegladarce)
- Wymaga: `kernel.perf_event_paranoid <= 1` lub fallback na `-e itimer`
- Na ARM (Raspberry Pi): uzyc build `linux-arm64`

## 2. Spring Boot Actuator — metryki HTTP i JVM

### Decision
Dodac `spring-boot-starter-actuator` jako zaleznosc. Eksponowac metryki przez endpoint `/actuator/metrics`.

### Rationale
- Wbudowane metryki Spring MVC: request count, latency per endpoint
- JVM metryki: heap, GC, threads — bez dodatkowej konfiguracji
- Bezpieczne: endpointy mozna ograniczyc do `health,metrics` (bez `env`, `beans` itp.)
- Integracja z Micrometer — timer per endpoint automatycznie

### Alternatives considered
- **Prometheus + Grafana**: Pelen stack monitoringu — overkill dla dev profiling
- **Custom metryki (reczne timery)**: Wymaga instrumentacji kazdego endpointu — wiecej kodu

### Kluczowe metryki do odczytu
- `http.server.requests` — count, totalTime, max per URI/method/status
- `jvm.memory.used` / `jvm.memory.max` — heap per area
- `jvm.gc.pause` — GC pause count + totalTime
- `jvm.threads.live` — active threads

### Bezpieczenstwo
- NIE eksponowac `/actuator/env` (moze ujawnic tokeny)
- Ograniczyc `management.endpoints.web.exposure.include` do minimum
- Rozwazyc `@Profile("dev")` — ale Actuator i tak jest kontrolowany przez konfiguracje

## 3. Hibernate Statistics — metryki SQL

### Decision
Wlaczyc `hibernate.generate_statistics=true` w profilu dev. Odczytywac statystyki przez `EntityManagerFactory` / `SessionFactory`.

### Rationale
- Zero dodatkowych zaleznosci (Hibernate juz jest)
- Metryki: query count, query execution max time, entity load/update/delete counts
- Mozliwosc resetu statystyk (wazne dla profilowania per-request)

### Alternatives considered
- **p6spy (SQL proxy logging)**: Loguje kazde query — duzo danych, trudne do agregacji
- **Datasource Proxy**: Wrapper na DataSource z licznikami — dodatkowa zaleznosc

### Kluczowe statystyki
- `Statistics.getQueryExecutionCount()` — calkowita liczba zapytan
- `Statistics.getQueryExecutionMaxTime()` — najwolniejsze query
- `Statistics.getQueryExecutionMaxTimeQueryString()` — SQL najwolniejszego query
- `Statistics.getEntityLoadCount()` — ile entity zaladowano (N+1 indicator)

### Implementacja w DiagnosticsController
```java
@Autowired EntityManagerFactory emf;

SessionFactory sf = emf.unwrap(SessionFactory.class);
Statistics stats = sf.getStatistics();
// stats.getQueryExecutionCount(), stats.clear(), etc.
```

## 4. Generowanie obciazenia testowego

### Decision
Prosty bash script z `curl` — sekwencja typowych operacji REST. Bez dodatkowych narzedzi load testowych.

### Rationale
- Profilowanie wymaga realistycznego obciazenia, ale nie stress testu
- `curl` w petli wystarczy do wygenerowania 10-30 requestow
- Nie potrzebujemy JMeter/Gatling/wrk — to dev profiling, nie benchmark
- Mock provider (bez realnych wywolan GitHub) — uniknac rate limitow

### Alternatives considered
- **wrk/hey**: HTTP benchmark tools — generuja tysiace requestow, tu potrzebujemy 10-30
- **JMeter/Gatling**: Ciezkie narzedzia load testowe — overkill
- **Custom Java load generator**: Wiecej kodu niz prosty curl script

### Scenariusz obciazenia
1. `POST /api/browse` — pobierz MR (mock)
2. `POST /api/analysis` — analizuj (mock provider, bez LLM)
3. `GET /api/analysis` — lista raportow
4. `GET /api/analysis/{id}` — szczegoly raportu
5. `GET /api/summary/{id}` — podsumowanie
6. Powtorz N razy z M concurrent (xargs -P)

## 5. Komenda Claude `/profile`

### Decision
Stworzyc `.claude/commands/profile.md` jako prompt komende. Skrypt bash `scripts/profile.sh` jako glowna logika.

### Rationale
- Komenda Claude to prompt template — wywoluje narzedzia (Bash)
- Logika w bash script — latwiej testowac i debugowac niz w prompcie
- Skrypt moze byc rowniez uruchomiony recznie bez Claude

### Struktura komendy
1. `/profile` → Claude czyta `profile.md` prompt
2. Prompt instruuje Claude aby uruchomil `scripts/profile.sh`
3. Skrypt zbiera metryki, generuje raw data
4. Claude analizuje dane i generuje raport Markdown z rekomendacjami

### Parametry (przekazywane do skryptu)
- `--duration 30` — czas profilowania async-profiler (default: 30s)
- `--with-load` — generuj obciazenie curl'em
- `--type cpu|alloc|full` — typ profilowania (default: full)
- `--load-iterations 10` — liczba powtorzen obciazenia
- `--load-concurrency 3` — rownolegle requesty
