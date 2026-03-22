# Data Model: 012 — Performance Profiling

## Uwaga

Ten feature NIE dodaje nowych encji domenowych ani tabel w bazie danych. Profilowanie jest narzedziem deweloperskim, ktore operuje poza domena biznesowa mr_analizer.

## Nowe struktury danych (DTO only, nie JPA)

### SqlStatsResponse (REST DTO)

Odpowiedz z endpointu `GET /api/diagnostics/sql-stats`:

```
SqlStatsResponse
├── queryExecutionCount: long          # calkowita liczba zapytan SQL
├── queryExecutionMaxTime: long        # najdluzsze query (ms)
├── queryExecutionMaxTimeQuery: String # SQL najdluzszego query
├── entityLoadCount: long              # ile entity zaladowano
├── entityInsertCount: long            # ile entity wstawiono
├── entityUpdateCount: long            # ile entity zaktualizowano
├── entityDeleteCount: long            # ile entity usunieto
├── collectionLoadCount: long          # ile kolekcji zaladowano (N+1 indicator)
├── secondLevelCacheHitCount: long     # trafienia cache L2
├── secondLevelCacheMissCount: long    # pudle cache L2
└── sessionOpenCount: long             # ile sesji otwarto
```

### Raport (plik Markdown, nie encja)

Generowany przez skrypt + Claude, zapisywany w `reports/profile-{timestamp}.md`:

```
Profile Report
├── metadata
│   ├── timestamp: datetime
│   ├── duration: int (seconds)
│   ├── loadGenerated: boolean
│   ├── profilingType: cpu|alloc|full
│   ├── serverPid: int
│   └── jvmVersion: string
├── cpuHotspots[]
│   ├── rank: int
│   ├── method: string
│   ├── cpuPercent: double
│   └── samples: int
├── memory
│   ├── heapUsed: long (bytes)
│   ├── heapMax: long (bytes)
│   ├── gcPauseTotalMs: long
│   └── gcPauseCount: int
├── httpLatency[]
│   ├── endpoint: string (method + uri)
│   ├── p50ms: double
│   ├── p95ms: double
│   ├── p99ms: double
│   └── count: int
├── sqlAnalysis
│   ├── totalQueries: long
│   ├── queriesPerEndpoint: Map<string, long>
│   ├── slowestQuery: string
│   ├── slowestQueryTimeMs: long
│   └── n1Warnings: string[]
└── recommendations[]
    ├── severity: HIGH|MEDIUM|LOW
    ├── title: string
    ├── problem: string
    ├── impact: string
    └── suggestedFix: string
```

## Relacje z istniejacym modelem

Brak — profilowanie nie dotyka domeny biznesowej. Jedyna interakcja to:
- DiagnosticsController czyta Hibernate Statistics z EntityManagerFactory
- Actuator automatycznie instrumentuje istniejace REST endpointy (zero zmian w controllerach)
