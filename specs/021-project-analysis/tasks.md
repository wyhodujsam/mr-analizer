# 021: Tasks — Project Analysis (AI Potential + BDD + SDD)

**Input**: `specs/021-project-analysis/spec.md`, `specs/021-project-analysis/plan.md`

## Phase 1: BDD Feature File (test-first)

- [ ] T01 Feature file `project-analysis.feature` — scenariusze: analiza projektu, AI scoring breakdown, BDD detection, SDD detection, drill-down, filtry/sortowanie

**Checkpoint**: Feature file gotowy, testy RED.

---

## Phase 2: Domain Models (pure Java)

- [ ] T02 `DetectionPatterns` record w `domain/model/project/DetectionPatterns.java` — bddPatterns, sddPatterns
- [ ] T03 `PrAnalysisRow` record w `domain/model/project/PrAnalysisRow.java` — prId, title, author, state, url, createdAt, mergedAt, additions, deletions, aiScore, aiVerdict, ruleResults, llmComment, hasBdd, hasSdd, bddFiles, sddFiles
- [ ] T04 `RuleFrequency` record w `domain/model/project/RuleFrequency.java` — ruleName, matchCount, avgWeight
- [ ] T05 `ScoreHistogramBucket` record w `domain/model/project/ScoreHistogramBucket.java` — rangeStart, rangeEnd, count
- [ ] T06 `ProjectSummary` record w `domain/model/project/ProjectSummary.java` — totalPrs, verdictCounts+percents, avgScore, topRules, histogram, bddCount/Percent, sddCount/Percent
- [ ] T07 `ProjectAnalysisResult` record w `domain/model/project/ProjectAnalysisResult.java` — projectSlug, analyzedAt, rows, summary

**Checkpoint**: Modele kompilują się.

---

## Phase 3: Domain Service — ArtifactDetector

- [ ] T08 `ArtifactDetector` w `domain/service/project/ArtifactDetector.java`:
  - `hasBdd(List<ChangedFile>)`, `hasSdd(List<ChangedFile>)`
  - `findBddFiles(List<ChangedFile>)`, `findSddFiles(List<ChangedFile>)`
  - Glob matching: `*.feature` → endsWith `.feature`, `*Steps.java` → endsWith `Steps.java`, `spec.md` → endsWith `/spec.md` or equals `spec.md`

- [ ] T09 `ArtifactDetectorTest`:
  - BDD: `.feature` → true, `Steps.java` → true, `_steps.py` → true, zwykły `.java` → false
  - SDD: `specs/001/spec.md` → true, `plan.md` → true, `README.md` → false
  - Mixed: `.feature` + `spec.md` → hasBdd=true, hasSdd=true
  - Empty files → false, false
  - `findBddFiles` returns matching paths
  - Nested paths: `src/test/features/login.feature` matches `*.feature`

**Checkpoint**: Detector przetestowany, green.

---

## Phase 4: Domain Service — ProjectAnalysisService

- [ ] T10 Port in: `ProjectAnalysisUseCase` w `domain/port/in/project/ProjectAnalysisUseCase.java`

- [ ] T11 Port out: nowa metoda `fetchFiles(String projectSlug, String mrId)` w `MergeRequestProvider` → `List<ChangedFile>`

- [ ] T12 `ProjectAnalysisService` w `domain/service/project/ProjectAnalysisService.java`:
  - Zależności: `ActivityAnalysisService` (cache), `MergeRequestProvider` (files), `ScoringEngine`, `List<Rule>`, `ArtifactDetector`, `LlmAnalyzer`
  - `analyzeProject(projectSlug, useLlm)`:
    1. `activityService.getOrFetchCache(projectSlug)` → allPrs
    2. Parallel: per PR → `analyzeOnePr()` (bounded executor)
    3. `buildSummary(rows)`
  - `analyzeOnePr(projectSlug, mr, useLlm)`:
    1. `fetchFiles(projectSlug, mr.getExternalId())`
    2. `scoringEngine.evaluate(mrWithFiles, rules, llmAssessment)`
    3. `artifactDetector.hasBdd/hasSdd(files)`
    4. Return `PrAnalysisRow` z ruleResults, bddFiles, sddFiles
  - `buildSummary(rows)`:
    - Verdict counts + percents
    - avgScore
    - Top rules: flatten ruleResults → groupBy ruleName → count + avgWeight → sort desc → limit 10
    - Histogram: 5 buckets [0-0.2, 0.2-0.4, ..., 0.8-1.0]
    - BDD/SDD counts + percents

- [ ] T13 `ProjectAnalysisServiceTest`:
  - Mock providers, 5 PR-ów z różnymi cechami
  - Verify: fetchFiles called per PR, scoring called, detection called
  - Summary: correct counts, percents, avgScore
  - Top rules: correct aggregation
  - Histogram: correct bucket distribution
  - Empty repo → empty result
  - useLlm=false → LlmAssessment.none()

**Checkpoint**: Service przetestowany, green.

---

## Phase 5: Adapter — config + GitHub + REST

- [ ] T14 `GitHubAdapter.fetchFiles(projectSlug, mrId)` — delegacja do `GitHubClient.fetchFiles()`, mapping `GitHubFile` → `ChangedFile` (mapping już istnieje w `toDomain`)

- [ ] T15 Detection patterns w `application.yml`:
  ```yaml
  mr-analizer:
    detection:
      bdd-patterns:
        - "*.feature"
        - "*Steps.java"
        - "*_steps.py"
        - "*_steps.rb"
        - "*.steps.ts"
        - "*Steps.kt"
      sdd-patterns:
        - "spec.md"
        - "plan.md"
        - "tasks.md"
        - "research.md"
        - "quickstart.md"
        - "checklist.md"
  ```

- [ ] T16 `ProjectConfig` w `adapter/config/ProjectConfig.java`:
  - Bean `DetectionPatterns` (czyta z yml)
  - Bean `ArtifactDetector`
  - Bean `ProjectAnalysisService`

- [ ] T17 `ProjectAnalysisResponse` DTO w `adapter/in/rest/project/dto/`:
  - Nested records: `SummaryResponse`, `PrRowResponse`, `RuleFrequencyResponse`, `HistogramBucketResponse`, `RuleResultResponse`
  - Static factory `from(ProjectAnalysisResult)`

- [ ] T18 `ProjectAnalysisController` w `adapter/in/rest/project/`:
  - `POST /api/project/{owner}/{repo}/analyze?useLlm=false`
  - Walidacja slug (reużycie SLUG_PART pattern)
  - Response: `ProjectAnalysisResponse`

**Checkpoint**: API działa, JSON poprawny.

---

## Phase 6: Frontend

- [ ] T19 Typy TypeScript `types/project.ts`:
  - `PrAnalysisRow`, `RuleFrequency`, `ScoreHistogramBucket`, `ProjectSummary`, `ProjectAnalysisResult`, `RuleResultItem`

- [ ] T20 API `api/projectApi.ts`:
  - `analyzeProject(owner, repo, useLlm): Promise<ProjectAnalysisResult>`

- [ ] T21 `ProjectAnalysisPage.tsx`:
  - Repo selector (reużycie)
  - Przycisk "Analizuj projekt" + checkbox "Użyj LLM (kosztowne)"
  - Loading state
  - Po załadowaniu: summary cards + tabela

- [ ] T22 Karta AI Potential (`AiPotentialCard.tsx`):
  - Donut chart (3 segmenty verdict) z % w środku — SVG lub CSS
  - Średni score, counts per verdict
  - Top 5 rules: mini tabela (name, count, avg weight)
  - Histogram: SVG bar chart (5 bucketów)

- [ ] T23 Karty BDD + SDD (`BddSddCards.tsx`):
  - BDD: % + count + badge kolor (>50 green, 20-50 yellow, <20 red)
  - SDD: analogicznie

- [ ] T24 Tabela z drill-down (`ProjectPrTable.tsx`):
  - Kolumny: title, author, date, AI score (badge), verdict (badge), BDD (✓/✗), SDD (✓/✗)
  - Sortowanie: klik nagłówek → asc/desc
  - Filtry: dropdown verdict, BDD, SDD
  - Expandable row (klik → rozwiń panel):
    - Score breakdown: tabela ruleResults
    - BDD files / SDD files lista
    - PR details: branch, size, url

- [ ] T25 Route + nav:
  - `/project` i `/project/:owner/:repo` w `App.tsx`
  - Link "Analiza projektu" w `Layout.tsx` navbar

- [ ] T26 Frontend testy (Vitest):
  - `AiPotentialCard.test.tsx` — procenty, top rules, histogram
  - `BddSddCards.test.tsx` — procenty, kolory badge
  - `ProjectPrTable.test.tsx` — sortowanie, filtrowanie, drill-down expand
  - `ProjectAnalysisPage.test.tsx` — loading, render po załadowaniu

**Checkpoint**: UI kompletne, testy green.

---

## Phase 7: BDD Steps + Integration + Polish

- [ ] T27 BDD step definitions `ProjectAnalysisSteps.java`
- [ ] T28 Run ALL tests: backend mvn test + frontend vitest run
- [ ] T29 Update CLAUDE.md — sekcja Project Analysis
- [ ] T30 Update SPEC.md — feature 021, nowe endpointy, test count

**Checkpoint**: Wszystkie testy green, docs aktualne.

---

## Dependency Graph

```
T01 (BDD file)
  ↓
T02-T07 (models) → T08-T09 (ArtifactDetector)
  ↓
T10-T11 (ports) → T12-T13 (service + tests)
  ↓
T14-T18 (adapter: GitHub, config, REST)
  ↓
T19-T26 (frontend: types, API, page, components, tests)
  ↓
T27-T30 (BDD steps, integration, docs)
```

## Szacunek złożoności

| Phase | Tasks | Złożoność |
|-------|-------|-----------|
| 1 — BDD file | T01 | S |
| 2 — Models | T02-T07 | S |
| 3 — ArtifactDetector | T08-T09 | S |
| 4 — Service | T10-T13 | M (orchestration + summary aggregation) |
| 5 — Adapter + REST | T14-T18 | M |
| 6 — Frontend | T19-T26 | L (karty, histogram, tabela z drill-down, filtry, sortowanie) |
| 7 — Polish | T27-T30 | S |
