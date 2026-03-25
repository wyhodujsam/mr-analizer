# 020: Cache aktywności + metryki wydajności kontrybutora

**Feature Branch**: `020-activity-cache-velocity`
**Created**: 2026-03-25
**Status**: Draft
**Input**: (1) Dodanie cache PR/review w module aktywności — analogicznie do BrowseMrService w głównej funkcjonalności. (2) Metryki wydajności kontrybutora (velocity, development impact, cycle time, code churn).

## Problem

### Brak cache w module aktywności

Moduł aktywności (015) przy każdym request odpytuje GitHub API od zera:
- `GET /pulls?state=all` (lista, paginacja) — 1+ calls
- `GET /pulls/{number}` (detail) — N calls (parallel)
- `GET /pulls/{number}/reviews` — M calls (parallel, merged only)

Przy repozytorium z 50 PR-ami jednego autora = ~100 API calls **za każdym requestem**. Przełączanie między kontrybutorami tego samego repo ponawia wszystkie calle. Rate limit GitHub: 5000/h — kilka raportów na dużym repo może wyczerpać.

Główna funkcjonalność (BrowseMrService) ma cache ConcurrentHashMap na browse results — moduł aktywności nie korzysta z niego ani nie ma własnego.

### Brak metryk wydajności

Obecny moduł aktywności wykrywa **nieprawidłowości** (flagi), ale nie mierzy **wydajności**. Tech lead/engineering manager potrzebuje metryk takich jak:
- Ile PR-ów merguje kontrybutor tygodniowo (velocity)?
- Jaki jest sumaryczny wpływ na codebase (development impact)?
- Jak szybko przechodzi PR od utworzenia do merge (cycle time)?
- Ile kodu jest refaktoryzowane vs dodawane (code churn)?
- Czy kontrybutor robi review innym (review engagement)?

Te metryki korzystają z **tych samych danych** co moduł flag — PR details + reviews. Cache jest warunkiem wstępnym, żeby obliczanie metryk nie powodowało dodatkowych API calls.

## User Scenarios & Testing

### User Story 1 — Cache danych PR dla modułu aktywności (Priority: P1)

System cachuje dane PR (lista + detale + reviews) per repozytorium, tak że kolejne requesty (inny autor, odświeżenie) nie powodują ponownych GitHub API calls.

**Why this priority**: Warunek wstępny — bez cache metryki wydajności podwoiłyby liczbę API calls.

**Independent Test**: Pobierz raport autora A → pobierz raport autora B tego samego repo → weryfikuj że nie było nowych GitHub API calls.

**Acceptance Scenarios**:

1. **Given** pierwszy request na raport aktywności repo X, **When** system buduje raport, **Then** fetchuje dane z GitHub API i cachuje je (lista PR + detale + reviews).
2. **Given** dane repo X w cache, **When** przychodzi request na raport innego autora tego samego repo, **Then** system używa danych z cache — zero API calls.
3. **Given** dane repo X w cache, **When** użytkownik klika "Odśwież dane", **Then** cache jest inwalidowany i dane pobrane ponownie z GitHub API.
4. **Given** dane repo X w cache, **When** mija 15 minut od ostatniego pobrania, **Then** system invaliduje cache automatycznie (TTL).
5. **Given** cache z danymi, **When** system zwraca listę kontrybutorów, **Then** korzysta z cache (nie fetchuje listy PR ponownie).

---

### User Story 2 — Velocity: tempo mergowania PR-ów (Priority: P1)

Dashboard wyświetla metrykę velocity — liczbę zmergowanych PR-ów w jednostce czasu (tygodniowo, w ostatnich 4 tygodniach, trend).

**Why this priority**: Podstawowa i najczęściej używana metryka produktywności — prosta do obliczenia z istniejących danych.

**Independent Test**: Kontrybutor z 12 PR-ami w 4 tygodniach → velocity = 3.0 PR/tydzień.

**Acceptance Scenarios**:

1. **Given** kontrybutor z 8 zmergowanymi PR-ami w ostatnich 4 tygodniach, **When** dashboard się ładuje, **Then** velocity = 2.0 PR/tydzień.
2. **Given** kontrybutor bez zmergowanych PR-ów w ostatnich 4 tygodniach, **When** dashboard się ładuje, **Then** velocity = 0.0 PR/tydzień.
3. **Given** kontrybutor z PR-ami rozłożonymi nierówno (6 w pierwszym tygodniu, 0 w pozostałych), **When** dashboard się ładuje, **Then** velocity = 1.5 PR/tydzień (średnia z 4 tygodni), a trend widocznie spadkowy.

---

### User Story 3 — Cycle Time: czas od utworzenia PR do merge (Priority: P1)

Dashboard wyświetla średni, medianowy i 90-percentylowy czas od otwarcia PR do merge (w godzinach).

**Why this priority**: Kluczowa metryka DORA — czas lead-to-deploy zaczyna się od czasu przejścia PR.

**Independent Test**: 5 PR-ów z czasami merge [1h, 2h, 3h, 24h, 48h] → median = 3h, avg = 15.6h, p90 = 48h.

**Acceptance Scenarios**:

1. **Given** kontrybutor z 5 zmergowanymi PR-ami o różnych czasach merge, **When** dashboard się ładuje, **Then** widać średnią, medianę i p90 cycle time w godzinach.
2. **Given** kontrybutor bez zmergowanych PR-ów, **When** dashboard się ładuje, **Then** cycle time = "brak danych".
3. **Given** PR-y z outlierem (1 PR mergowany po 30 dniach), **When** dashboard się ładuje, **Then** mediana jest stabilna, ale p90 i średnia wyraźnie wyższe — użytkownik widzi discrepancy.

---

### User Story 4 — Development Impact: wpływ na codebase (Priority: P2)

Dashboard wyświetla sumaryczny i średni wpływ kontrybutora mierzony rozmiarem zmian (additions + deletions).

**Why this priority**: Uzupełnia velocity — dużo małych PR-ów vs mało dużych PR-ów to inna wydajność.

**Independent Test**: 3 PR-y (100, 200, 300 lines) → total impact = 600, avg = 200 lines/PR.

**Acceptance Scenarios**:

1. **Given** kontrybutor z 3 PR-ami o rozmiarach 100, 200, 500 linii, **When** dashboard się ładuje, **Then** total impact = 800, avg impact = 267 lines/PR.
2. **Given** kontrybutor bez PR-ów, **When** dashboard się ładuje, **Then** impact = 0.
3. **Given** kontrybutor z PR-ami, **When** dashboard się ładuje, **Then** widać rozkład: additions vs deletions (proporcja add/delete).

---

### User Story 5 — Code Churn: refactoring vs nowy kod (Priority: P2)

Dashboard wyświetla ratio deletions / additions. Wysoki churn = dużo refaktoringu/usuwania; niski = głównie nowy kod.

**Why this priority**: Ciekawa metryka zdrowia codebase — zespół który nigdy nie usuwa kodu to red flag.

**Independent Test**: 500 additions + 300 deletions → churn ratio = 0.60 (60% usunięć na dodanie).

**Acceptance Scenarios**:

1. **Given** kontrybutor z sumarycznie 1000 additions i 400 deletions, **When** dashboard się ładuje, **Then** churn ratio = 0.40 (40%).
2. **Given** kontrybutor z samymi additions (0 deletions), **When** dashboard się ładuje, **Then** churn ratio = 0.00 — label "Głównie nowy kod".
3. **Given** kontrybutor z więcej deletions niż additions, **When** dashboard się ładuje, **Then** churn ratio > 1.0 — label "Przewaga refaktoringu/usuwania".

---

### User Story 6 — Review Engagement: aktywność w review (Priority: P3)

Dashboard wyświetla ile review dał kontrybutor innym osobom vs ile review otrzymał na swoich PR-ach.

**Why this priority**: Najniższy priorytet — wymaga dodatkowego fetch reviews cudzych PR-ów, co jest kosztowne. Ale ważna metryka zespołowa.

**Independent Test**: Kontrybutor dał 10 review, otrzymał 15 → ratio given/received = 0.67.

**Acceptance Scenarios**:

1. **Given** kontrybutor który dał 8 review i otrzymał 12, **When** dashboard się ładuje, **Then** review engagement: given=8, received=12, ratio=0.67.
2. **Given** kontrybutor bez żadnych review (ani danych ani otrzymanych), **When** dashboard się ładuje, **Then** review engagement: given=0, received=0, ratio="brak danych".
3. **Given** kontrybutor który daje dużo review ale nie dostaje ich na swoich PR-ach, **When** dashboard się ładuje, **Then** ratio > 1.0 — label "Aktywny reviewer".

## Scope & Constraints

### In scope
- Cache PR data per-repo (lista + detale + reviews) z TTL 15 min
- Invalidacja cache (manualna + TTL)
- 5 metryk wydajności: velocity, cycle time, development impact, code churn, review engagement
- Wyświetlanie metryk na istniejącym dashboardzie aktywności (sekcja "Wydajność")
- BDD scenariusze .feature PRZED implementacją
- Unit testy metryk

### Out of scope
- Persystencja raportów aktywności do bazy (future feature)
- Porównanie kontrybutorów (future feature)
- Metryki zespołowe (agregat per team)
- Historyczne trendy (wymaga persystencji)
- Eksport metryk do CSV/PDF

## Technical Notes

- Cache powinien być na poziomie **repo** (nie per-autor) — dane PR to dane repo, filtrowanie per-autor jest lokalne
- Cache key: `projectSlug` (prostsze niż FetchCriteria.cacheKey() z browse)
- Reviews fetch: cache ALL reviews dla merged PRs, nie per-autor
- Velocity window: 4 tygodnie (configurable w application.yml)
- Cycle time: tylko merged PRs (open/closed nie mają mergedAt)
- Review engagement: wymaga przeglądania reviews **wszystkich** PR-ów (nie tylko autora) — expensive, P3
- Metryki obliczane z danych w cache (zero dodatkowych API calls po pierwszym pobraniu)
