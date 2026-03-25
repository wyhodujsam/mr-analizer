# 022: Plan — Uzupełnienie luk w pokryciu testami

**Branch**: `022-test-coverage-gaps` | **Date**: 2026-03-25 | **Spec**: `specs/022-test-coverage-gaps/spec.md`

## Summary

Uzupełnienie 10 luk w piramidzie testów. Zero nowej funkcjonalności — wyłącznie testy. Priorytet: integration REST → JPA round-trip → E2E → unit gaps.

## Technical Context

**Testy do dodania**: ~40-50 nowych test cases
**Frameworki**: JUnit 5 + MockWebServer (integration), Playwright (E2E), Vitest (frontend), Spring Boot Test (JPA)
**Infrastruktura**: MockWebServer z fixtures JSON (istniejący wzorzec z `GitHubIntegrationTestBase`)

## Kolejność implementacji

Priorytety: P1 (blokujące) → P2 (ważne) → P3 (nice-to-have).

### Phase 1 — BDD feature file (test-first)

1. **`test-coverage.feature`** — scenariusze BDD dla nowych testów integracyjnych (project analysis REST round-trip)

### Phase 2 — Integration testy REST (P1)

2. **`ProjectAnalysisIntegrationTest`** — extends `GitHubIntegrationTestBase`:
   - POST `/api/project/{owner}/{repo}/analyze` → 200 z JSON (summary, rows, ruleResults, BDD/SDD)
   - GET `/api/project/{owner}/{repo}/analyses` → lista zapisanych
   - GET `/api/project/analyses/{id}` → konkretna analiza
   - DELETE `/api/project/analyses/{id}` → 204
   - GET `/api/project/analyses/999` → 404
   - POST z invalid slug → 400
   - POST na empty repo (0 PR) → 200 z pustym wynikiem
   - Fixtures: reużycie istniejących `pr-list-3.json`, `pr-single-*.json`, `pr-files-*.json`

3. **`ActivityCacheIntegrationTest`** — extends `GitHubIntegrationTestBase`:
   - POST `/api/activity/{owner}/{repo}/refresh` → 204
   - DELETE `/api/activity/{owner}/{repo}/cache` → 204
   - Weryfikacja: po refresh, kolejny GET report zwraca świeże dane

### Phase 3 — JPA persistence round-trip (P1)

4. **`JpaProjectAnalysisRepositoryIntegrationTest`** — `@SpringBootTest` z H2:
   - save ProjectAnalysisResult z 3 PR-ami (ruleResults, Verdict, BDD/SDD files, LocalDateTime) → findById → assert equal
   - findByProjectSlug → lista
   - deleteById → findById = empty
   - Weryfikacja JSON serializacji: Verdict enum, List<RuleResult>, nullable fields

### Phase 4 — SSE streaming test (P2)

5. **`ProjectAnalysisSseTest`** — unit test controllera:
   - Mock `ProjectAnalysisUseCase`, verify SSE events emitted
   - Lub integration test z `TestRestTemplate` + SSE parsing
   - Prostsza opcja: test service z progress callback (verify callback wywoływany N razy)

### Phase 5 — Concurrent guard test (P2)

6. **`ProjectAnalysisServiceConcurrencyTest`** — unit test:
   - Uruchom analizę na wątku → natychmiast uruchom drugą na tym samym slug → expect IllegalStateException
   - Uruchom na innym slug → OK
   - Po zakończeniu pierwszej → ponowna analiza OK

### Phase 6 — E2E Playwright (P1-P2)

7. **`project-analysis.spec.ts`** — E2E:
   - Nawigacja na `/project`
   - Wybór repo → "Analizuj projekt" → progress bar → wynik (karty + tabela)
   - Drill-down: klik wiersz → score breakdown
   - Zapisane analizy: lista → klik → załadowany wynik

8. **`activity-tabs.spec.ts`** — E2E:
   - Nawigacja na `/activity`
   - Wybór repo + kontrybutor → 3 zakładki
   - Klik "Wydajność" → metryki
   - Klik "Naruszenia" → flagi z badge

### Phase 7 — Unit gaps (P2-P3)

9. **`ProjectAnalysisServiceEnrichTest`** — unit test `enrichWithFiles`:
   - Oryginalne pola zachowane
   - hasTests=true gdy pliki test
   - hasTests=false gdy brak

10. **`ProjectAnalysisNegativeTest`** — unit test:
    - Invalid slug → InvalidRequestException
    - fetchFiles rzuca exception → graceful fallback (empty files)
    - LLM timeout → graceful fallback

### Phase 8 — Frontend test (P2)

11. **`ProjectAnalysisPage.test.tsx`** — Vitest:
    - Render z mock API (saved analyses)
    - Loading state
    - Error state
    - Progress bar rendering
    - Klik "Odśwież analizę"

### Phase 9 — BDD steps + all green

12. BDD step definitions dla nowych scenariuszy
13. Run ALL tests: `mvn test` + `npx vitest run` + `npx playwright test`

## Dependency Graph

```
Phase 1 (BDD file)
  ↓
Phase 2 (REST integration) ←── reużycie MockWebServer fixtures
Phase 3 (JPA round-trip) ←── Spring Boot Test + H2
  ↓
Phase 4 (SSE test)
Phase 5 (Concurrency test)
  ↓
Phase 6 (E2E Playwright) ←── wymaga running backend + frontend
  ↓
Phase 7 (Unit gaps)
Phase 8 (Frontend test)
  ↓
Phase 9 (BDD steps + all green)
```

## Szacunek

| Phase | Testy | Złożoność |
|-------|-------|-----------|
| 1 — BDD file | 1 file | S |
| 2 — REST integration | ~8 testów | M (fixtures setup) |
| 3 — JPA round-trip | ~4 testy | S |
| 4 — SSE test | ~2 testy | M |
| 5 — Concurrency | ~3 testy | M (threading) |
| 6 — E2E | ~8 testów | L (Playwright + route mocking) |
| 7 — Unit gaps | ~5 testów | S |
| 8 — Frontend test | ~5 testów | M |
| 9 — Integration | — | S |

**Total: ~40 nowych testów**
