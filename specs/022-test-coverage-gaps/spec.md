# 022: Uzupełnienie luk w pokryciu testami

**Feature Branch**: `022-test-coverage-gaps`
**Created**: 2026-03-25
**Status**: Draft
**Input**: Code review wykazał 10 luk w piramidzie testów — brak integration testów REST, E2E nowych stron, SSE streaming, JPA round-trip, concurrent guard, negative cases.

## Problem

Piramida testów (~662 testy) ma solidną bazę unit + BDD, ale brakuje:
- **Integration testów REST** dla nowych modułów (Project Analysis, Activity cache endpoints)
- **E2E testów** nowych stron (`/project`, `/activity` z zakładkami)
- **Testów SSE** streaming endpoint
- **JPA persistence round-trip** dla ProjectAnalysis
- **Testów negatywnych** (invalid input, 404, timeout)
- **Testów concurrency** (guard na duplicate analysis)

## User Scenarios & Testing

### US1 — Integration testy Project Analysis REST (Priority: P1)

**Acceptance Scenarios**:

1. **Given** MockWebServer z fixtures PR-ów, **When** POST `/api/project/{owner}/{repo}/analyze`, **Then** response 200 z poprawnym JSON (summary + rows + ruleResults).
2. **Given** zapisana analiza w H2, **When** GET `/api/project/{owner}/{repo}/analyses`, **Then** response 200 z listą analiz.
3. **Given** zapisana analiza, **When** GET `/api/project/analyses/{id}`, **Then** response 200 z pełnym wynikiem.
4. **Given** zapisana analiza, **When** DELETE `/api/project/analyses/{id}`, **Then** response 204, analiza usunięta.
5. **Given** nieistniejące id, **When** GET `/api/project/analyses/999`, **Then** response 404.

---

### US2 — Integration testy Activity cache endpoints (Priority: P2)

**Acceptance Scenarios**:

1. **Given** dane w cache, **When** POST `/api/activity/{owner}/{repo}/refresh`, **Then** response 204, cache odświeżony.
2. **Given** dane w cache, **When** DELETE `/api/activity/{owner}/{repo}/cache`, **Then** response 204, cache wyczyszczony.

---

### US3 — JPA persistence round-trip ProjectAnalysis (Priority: P1)

**Acceptance Scenarios**:

1. **Given** ProjectAnalysisResult z 5 PR-ami (ruleResults, Verdict enum, BDD/SDD files), **When** save + findById, **Then** zwrócony obiekt identyczny z zapisanym.
2. **Given** zapisana analiza, **When** findByProjectSlug, **Then** lista zawiera zapisaną analizę.
3. **Given** zapisana analiza, **When** deleteById + findById, **Then** Optional.empty().

---

### US4 — Test SSE streaming (Priority: P2)

**Acceptance Scenarios**:

1. **Given** repo z 3 PR-ami, **When** POST `/api/project/{owner}/{repo}/analyze-stream`, **Then** otrzymuję SSE events: 3× `progress` + 1× `result`.
2. **Given** stream w toku, **When** klient się rozłączy, **Then** serwer nie crashuje.

---

### US5 — E2E test strony /project (Priority: P1)

**Acceptance Scenarios**:

1. **Given** aplikacja działa, **When** nawiguję na `/project`, **Then** widzę repo selector i przycisk "Analizuj projekt".
2. **Given** wybrany repo, **When** klikam "Analizuj projekt", **Then** widzę progress bar → karty AI/BDD/SDD → tabelę PR-ów.
3. **Given** załadowany wynik, **When** klikam wiersz PR, **Then** widzę drill-down z score breakdown.
4. **Given** zapisana analiza, **When** odświeżam stronę, **Then** widzę listę zapisanych analiz.

---

### US6 — E2E test /activity z zakładkami (Priority: P2)

**Acceptance Scenarios**:

1. **Given** wybrany repo i kontrybutor, **When** dashboard się ładuje, **Then** widzę 3 zakładki.
2. **Given** zakładka "Wydajność", **When** klikam, **Then** widzę metryki velocity/cycle time.
3. **Given** zakładka "Naruszenia", **When** klikam, **Then** widzę listę flag z badge count.

---

### US7 — Test concurrent analysis guard (Priority: P2)

**Acceptance Scenarios**:

1. **Given** analiza repo X w toku, **When** drugi request na repo X, **Then** rzuca wyjątek / 409 Conflict.
2. **Given** analiza repo X w toku, **When** request na repo Y, **Then** przechodzi normalnie.

---

### US8 — Testy negatywne REST (Priority: P2)

**Acceptance Scenarios**:

1. **Given** invalid slug `../etc/passwd`, **When** POST analyze, **Then** 400 Bad Request.
2. **Given** nieistniejące repo, **When** POST analyze, **Then** 404 lub timeout z czytelnym błędem.
3. **Given** empty repo (0 PR-ów), **When** POST analyze, **Then** 200 z pustym wynikiem (0 rows, 0%).

---

### US9 — Test enrichWithFiles (Priority: P3)

**Acceptance Scenarios**:

1. **Given** MergeRequest z 10 polami, **When** enrichWithFiles, **Then** wszystkie oryginalne pola zachowane + changedFiles dodane.
2. **Given** pliki z testami, **When** enrichWithFiles, **Then** hasTests=true.

---

### US10 — Frontend test ProjectAnalysisPage (Priority: P2)

**Acceptance Scenarios**:

1. **Given** mock API, **When** strona się ładuje, **Then** widzę repo selector.
2. **Given** saved analyses w API, **When** wybieram repo, **Then** widzę listę zapisanych analiz.
3. **Given** analiza w toku, **When** progress events, **Then** widzę progress bar.
4. **Given** error z API, **When** analiza, **Then** widzę alert z błędem.

## Scope & Constraints

### In scope
- Integration testy REST (MockWebServer) dla Project Analysis + Activity cache
- JPA round-trip test z Spring context + H2
- E2E (Playwright) dla `/project` i `/activity` zakładki
- Unit test SSE (mock SseEmitter)
- Unit test concurrent guard
- Negative REST testy (invalid input, 404)
- Frontend test ProjectAnalysisPage
- Unit test enrichWithFiles

### Out of scope
- Nowa funkcjonalność — TYLKO testy
- Refaktoring istniejącego kodu
- Performance testy / load testy
