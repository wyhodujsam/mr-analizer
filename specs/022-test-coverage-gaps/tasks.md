# 022: Tasks — Uzupełnienie luk w pokryciu testami

**Input**: `specs/022-test-coverage-gaps/spec.md`, `specs/022-test-coverage-gaps/plan.md`

## Phase 1: BDD Feature File (test-first)

- [ ] T01 Feature file `test-coverage.feature` — scenariusze: project analysis REST round-trip (analyze → list → get → delete → 404 → invalid slug)

**Checkpoint**: Feature file ready, RED.

---

## Phase 2: Integration testy REST — Project Analysis (P1)

- [ ] T02 JSON fixtures dla project analysis:
  - Reużycie istniejących `pr-list-3.json`, `pr-single-*.json`, `pr-files-*.json`
  - Dodanie route dla `/pulls/{number}/files` jeśli brakuje w `ActivityIntegrationTest` dispatcher

- [ ] T03 `ProjectAnalysisIntegrationTest` extends `GitHubIntegrationTestBase`:
  - `POST /api/project/test/repo/analyze` → 200 z JSON: summary (totalPrs=3, verdictCounts, avgScore, topRules, histogram, bddCount, sddCount), rows (3 entries z ruleResults, hasBdd, hasSdd, bddFiles, sddFiles)
  - `GET /api/project/test/repo/analyses` → 200 z listą ≥1 analiz
  - `GET /api/project/analyses/{id}` → 200 z pełnym wynikiem
  - `DELETE /api/project/analyses/{id}` → 204
  - `GET /api/project/analyses/999` → 404
  - `POST /api/project/../repo/analyze` → 400 (invalid slug)
  - Verify: JSON zawiera ruleResults per PR, bddFiles/sddFiles jeśli match

**Checkpoint**: 7 integration testów green.

---

## Phase 3: Integration testy REST — Activity Cache (P2)

- [ ] T04 `ActivityCacheEndpointsIntegrationTest` extends `GitHubIntegrationTestBase`:
  - Wywołaj GET `/api/activity/test/repo/report?author=alice` (warms cache)
  - POST `/api/activity/test/repo/refresh` → 204
  - DELETE `/api/activity/test/repo/cache` → 204
  - Po DELETE, kolejny GET `/api/activity/test/repo/report?author=alice` → nowy fetch (weryfikuj przez MockWebServer request count)

**Checkpoint**: 3-4 testy green.

---

## Phase 4: JPA Persistence Round-Trip (P1)

- [ ] T05 `JpaProjectAnalysisRepositoryIntegrationTest` — `@SpringBootTest` z `@ActiveProfiles("test")`:
  - save `ProjectAnalysisResult` z: 3 `PrAnalysisRow` (Verdict.AUTOMATABLE, NOT_SUITABLE, MAYBE), `List<RuleResult>`, `List<String>` bddFiles/sddFiles, nullable llmComment, `LocalDateTime` createdAt/mergedAt
  - findById → assert: id assigned, projectSlug, analyzedAt, summary fields, rows count, Verdict enum correct, ruleResults preserved, bddFiles/sddFiles preserved
  - findByProjectSlug → lista ≥1
  - deleteById → findById = empty
  - save 2 analyses for same slug → findByProjectSlug returns 2 ordered by analyzedAt desc

**Checkpoint**: 5 testów green.

---

## Phase 5: SSE Streaming Test (P2)

- [ ] T06 `ProjectAnalysisServiceProgressTest` — unit test:
  - Mock providers (3 PR-y)
  - Call `analyzeProject(slug, false, progressCallback)`
  - Verify: progressCallback called 3 times z (1,3), (2,3), (3,3) (order may vary due to parallel)
  - Verify: all 3 invocations have total=3, processed ∈ {1,2,3}

- [ ] T07 `ProjectAnalysisSseIntegrationTest` (opcjonalny) — integration z `WebTestClient`:
  - POST `/api/project/test/repo/analyze-stream` z MockWebServer
  - Parse SSE events, verify: ≥1 `progress` event + 1 `result` event
  - **Uwaga**: SSE z POST wymaga WebTestClient lub raw HTTP — jeśli zbyt złożone, skip na rzecz T06

**Checkpoint**: 1-2 testy green.

---

## Phase 6: Concurrent Guard Test (P2)

- [ ] T08 `ProjectAnalysisServiceConcurrencyTest` — unit test:
  - Mock provider z `Thread.sleep(500)` w fetchFiles (symuluje wolny API)
  - Wątek 1: `analyzeProject("owner/repo", false)` (blokujący)
  - Wątek 2 (po 100ms): `analyzeProject("owner/repo", false)` → expect `IllegalStateException`
  - Wątek 3 (po 100ms): `analyzeProject("other/repo", false)` → OK (inny slug)
  - Po zakończeniu wątku 1: `analyzeProject("owner/repo", false)` → OK

**Checkpoint**: 3 testy green.

---

## Phase 7: E2E Playwright (P1-P2)

- [ ] T09 `project-analysis.spec.ts` — E2E:
  - Route interception: mock `/api/project/*/analyze-stream` (SSE response), `/api/project/*/analyses` (lista)
  - Test 1: nawigacja na `/project` → widoczny "Analiza projektu" heading, repo selector
  - Test 2: wybierz repo → "Analizuj projekt" → progress → wynik (karty AI/BDD/SDD + tabela)
  - Test 3: klik wiersz PR → drill-down z "Score Breakdown"
  - Test 4: "Odśwież analizę" → re-analyze
  - Test 5: lista zapisanych analiz → klik → załadowanie

- [ ] T10 `activity-tabs.spec.ts` — E2E:
  - Route interception: mock `/api/activity/*/contributors`, `/api/activity/*/report`
  - Test 1: nawigacja `/activity` → wybór repo + kontrybutor → 3 zakładki widoczne
  - Test 2: klik "Wydajność" → widoczne metryki (velocity, cycle time)
  - Test 3: klik "Naruszenia" → widoczna lista flag

**Checkpoint**: 8 E2E testów green.

---

## Phase 8: Unit Gaps (P2-P3)

- [ ] T11 `ProjectAnalysisServiceEnrichTest` — unit test:
  - `enrichWithFiles(mr, files)` → wszystkie oryginalne pola MR zachowane (externalId, title, author, state, description, sourceBranch, targetBranch, createdAt, mergedAt, updatedAt, labels, diffStats, provider, url, projectSlug)
  - hasTests=true gdy pliki zawierają "test"/"Test"
  - hasTests=false gdy brak testowych plików
  - changedFiles = files z argumentu

- [ ] T12 `ProjectAnalysisNegativeTest` — unit test:
  - fetchFiles rzuca exception → PR analizowany z empty files (graceful)
  - LLM analyzer rzuca exception → fallback LlmAssessment z error message
  - Empty repo (0 PR) → empty result, summary z zerami

**Checkpoint**: 5-6 testów green.

---

## Phase 9: Frontend Test — ProjectAnalysisPage (P2)

- [ ] T13 `ProjectAnalysisPage.test.tsx` — Vitest:
  - Mock `projectApi` (analyzeProjectWithProgress, getSavedAnalyses, deleteProjectAnalysis) + `analysisApi` (getRepos)
  - Test 1: render z MemoryRouter `/project/owner/repo` → repo selector + "Analizuj projekt" button
  - Test 2: klik "Analizuj projekt" → loading spinner visible
  - Test 3: po załadowaniu → karty AI/BDD/SDD + tabela
  - Test 4: lista saved analyses → klik → wynik załadowany
  - Test 5: error z API → alert visible

**Checkpoint**: 5 testów green.

---

## Phase 10: BDD Steps + All Green

- [ ] T14 BDD step definitions dla `test-coverage.feature`
- [ ] T15 Run ALL: `cd backend && mvn test` → green
- [ ] T16 Run ALL: `cd frontend && npx vitest run` → green
- [ ] T17 Run E2E: `cd frontend && npx playwright test` → green
- [ ] T18 Update test counts w SPEC.md

**Checkpoint**: Cała piramida green, docs aktualne.

---

## Dependency Graph

```
T01 (BDD file)
  ↓
T02-T03 (REST integration project) ─── T04 (REST activity cache)
  ↓
T05 (JPA round-trip)
  ↓
T06-T07 (SSE test) ─── T08 (concurrency)
  ↓
T09-T10 (E2E Playwright)
  ↓
T11-T12 (unit gaps) ─── T13 (frontend test)
  ↓
T14-T18 (BDD steps + all green)
```

## Podsumowanie wpływu na piramidę

| Warstwa | Przed | Dodane | Po |
|---------|-------|--------|----|
| Unit | ~555 | ~14 | ~569 |
| BDD | 70 | ~6 | ~76 |
| Integration | 26 | ~15 | ~41 |
| E2E | 11 | ~8 | ~19 |
| Frontend unit | 155 | ~5 | ~160 |
| **Total** | **~662** | **~48** | **~710** |
