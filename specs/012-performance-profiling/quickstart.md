# Quickstart: 012 — Performance Profiling

## Wymagania wstepne

1. Dzialajacy serwer mr_analizer (`cd backend && mvn spring-boot:run`)
2. async-profiler zainstalowany w `~/tools/async-profiler/`

## Instalacja async-profiler (jednorazowo)

```bash
mkdir -p ~/tools
cd ~/tools
# Linux x64:
wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
tar xzf async-profiler-3.0-linux-x64.tar.gz
# Linux ARM64 (Raspberry Pi itp.):
# wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-arm64.tar.gz
# tar xzf async-profiler-3.0-linux-arm64.tar.gz
```

Sprawdz kernel config:
```bash
# Jesli async-profiler nie moze uzyc perf_events:
echo 1 | sudo tee /proc/sys/kernel/perf_event_paranoid
# Lub na stale: sudo sysctl -w kernel.perf_event_paranoid=1
```

## Uzycie

### Przez komende Claude
```
/profile
/profile --duration 60 --with-load
/profile --type cpu
/profile --with-load --load-iterations 20 --load-concurrency 5
```

### Reczne uruchomienie skryptu
```bash
cd ~/mr_analizer
bash scripts/profile.sh --duration 30 --with-load --type full
```

## Wyniki

Raporty zapisywane w:
```
reports/
├── profile-2026-03-22-143000.md          # raport Markdown
└── flamegraphs/
    ├── cpu-1711234567.html                # CPU flame graph
    └── alloc-1711234567.html              # allocation flame graph
```

## Diagnostyka

```bash
# Sprawdz czy serwer dziala:
curl -s http://localhost:8083/actuator/health

# Metryki HTTP:
curl -s http://localhost:8083/actuator/metrics/http.server.requests

# Statystyki SQL:
curl -s http://localhost:8083/api/diagnostics/sql-stats

# Reset statystyk SQL:
curl -X POST http://localhost:8083/api/diagnostics/sql-stats/reset
```
