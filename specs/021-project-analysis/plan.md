# 021: Plan implementacji — Project Analysis (AI Potential + BDD + SDD)

**Branch**: `021-project-analysis` | **Date**: 2026-03-25 | **Spec**: `specs/021-project-analysis/spec.md`

## Summary

Nowa strona `/project` analizująca WSZYSTKIE PR-y repozytorium w trzech wymiarach: (1) AI Potential — ScoringEngine + szczegółowy breakdown (reguły, histogram, donut), (2) BDD detection — konfigurowalne wzorce plików, (3) SDD detection — konfigurowalne wzorce plików. Z drill-down do poszczególnych PR-ów (score breakdown, pliki BDD/SDD).

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.x, React 18 + TypeScript
**Reużycie**: ScoringEngine (domain), ActivityRepoCache (cache PR-ów), MergeRequestProvider, List<Rule>
**Storage**: In-memory (on-demand)
**Testing**: JUnit 5 + Mockito (unit), Cucumber 7 (BDD), Vitest + RTL (frontend)
**Performance**: Activity cache dla detali, files fetch parallel (bounded executor)

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | OK | Nowy port in (`ProjectAnalysisUseCase`), detection w domain |
| II. Provider Abstraction | OK | `fetchFiles` na porcie, adapter tłumaczy na GitHub/GitLab |
| III. BDD Testing | OK | Feature files PRZED implementacją |
| IV. SDD Workflow | OK | spec → plan → tasks → implement |
| V. Simplicity (YAGNI) | OK | Brak persystencji, brak trendów, brak eksportu |

## Kluczowe decyzje architektoniczne

### Skąd dane o plikach PR?

Activity cache (020) przechowuje detale PR-ów (single endpoint) z `DiffStats`, ale **nie listę plików**. `GET /pulls/{number}/files` to osobny call.

**Decyzja**: Nowa metoda `MergeRequestProvider.fetchFiles(projectSlug, mrId)` → adapter deleguje do `GitHubClient.fetchFiles()` (już istnieje). Pliki NIE cachowane — za dużo pamięci. Fetchowane on-demand, parallel (bounded executor 8 threads).

### Scoring bez LLM

ScoringEngine wymaga `LlmAssessment`. Domyślnie `LlmAssessment.none()` — pure rule-based. Checkbox na UI "Użyj LLM" z warning o koszcie.

### Score breakdown — skąd szczegóły

`ScoringEngine.evaluate()` zwraca `AnalysisResult` z `List<RuleResult>` — per-rule breakdown już istnieje. Dla drill-down: przechowujemy `ruleResults` per PR w `PrAnalysisRow`. Dla top-rules: agregujemy `ruleName` → count across all rows.

### Drill-down — expandable row vs osobna strona

**Decyzja**: Expandable row w tabeli (accordion). Kliknięcie wiersza rozwija panel z detalami. Mniej nawigacji, szybszy wgląd. Dane już są w response (ruleResults, bddFiles, sddFiles, MR metadata).

## Warstwy zmian

### Warstwa 1: Domain models (pure Java)

1. **`DetectionPatterns`** — konfigurowalne wzorce:
   ```java
   public record DetectionPatterns(
       List<String> bddPatterns,
       List<String> sddPatterns
   ) {}
   ```

2. **`PrAnalysisRow`** — pełny wiersz per PR (z danymi na drill-down):
   ```java
   public record PrAnalysisRow(
       String prId,
       String title,
       String author,
       String state,
       String url,
       LocalDateTime createdAt,
       LocalDateTime mergedAt,
       int additions,
       int deletions,
       // AI Potential
       double aiScore,
       Verdict aiVerdict,
       List<RuleResult> ruleResults,    // breakdown: per-rule score detail
       String llmComment,                // nullable, only if useLlm=true
       // BDD / SDD
       boolean hasBdd,
       boolean hasSdd,
       List<String> bddFiles,           // matched file paths
       List<String> sddFiles            // matched file paths
   ) {}
   ```

3. **`RuleFrequency`** — zagregowana reguła:
   ```java
   public record RuleFrequency(
       String ruleName,
       int matchCount,
       double avgWeight
   ) {}
   ```

4. **`ScoreHistogramBucket`** — bucket histogramu:
   ```java
   public record ScoreHistogramBucket(
       double rangeStart,    // 0.0, 0.2, 0.4, 0.6, 0.8
       double rangeEnd,      // 0.2, 0.4, 0.6, 0.8, 1.0
       int count
   ) {}
   ```

5. **`ProjectSummary`** — zagregowane statystyki + breakdown:
   ```java
   public record ProjectSummary(
       int totalPrs,
       // AI Potential
       int automatableCount,
       int maybeCount,
       int notSuitableCount,
       double automatablePercent,
       double maybePercent,
       double notSuitablePercent,
       double avgScore,
       List<RuleFrequency> topRules,          // top matched rules across all PRs
       List<ScoreHistogramBucket> histogram,   // score distribution
       // BDD
       int bddCount,
       double bddPercent,
       // SDD
       int sddCount,
       double sddPercent
   ) {}
   ```

6. **`ProjectAnalysisResult`** — wynik:
   ```java
   public record ProjectAnalysisResult(
       String projectSlug,
       LocalDateTime analyzedAt,
       List<PrAnalysisRow> rows,
       ProjectSummary summary
   ) {}
   ```

### Warstwa 2: Domain service — ArtifactDetector

7. **`ArtifactDetector`** — pure domain, glob matching na plikach:
   ```java
   public class ArtifactDetector {
       private final DetectionPatterns patterns;

       public boolean hasBdd(List<ChangedFile> files) { ... }
       public boolean hasSdd(List<ChangedFile> files) { ... }
       public List<String> findBddFiles(List<ChangedFile> files) { ... }
       public List<String> findSddFiles(List<ChangedFile> files) { ... }

       // Matching: "*.feature" matches any path ending with ".feature"
       // "spec.md" matches any path ending with "/spec.md" or exactly "spec.md"
       // "*Steps.java" matches any path ending with "Steps.java"
   }
   ```

### Warstwa 3: Domain service — ProjectAnalysisService

8. **`ProjectAnalysisService`** (implements `ProjectAnalysisUseCase`):
   ```java
   public ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm) {
       // 1. Get all PRs from activity cache
       ActivityRepoCache cache = activityService.getOrFetchCache(projectSlug);
       List<MergeRequest> allPrs = cache.getAllDetailedPrs();

       // 2. Parallel: per PR → fetchFiles + score + detect BDD/SDD
       List<PrAnalysisRow> rows = analyzeAllPrs(projectSlug, allPrs, useLlm);

       // 3. Build summary with breakdown
       ProjectSummary summary = buildSummary(rows);

       return new ProjectAnalysisResult(projectSlug, LocalDateTime.now(), rows, summary);
   }
   ```

   **`analyzeOnePr`** per PR:
   ```java
   PrAnalysisRow analyzeOnePr(String projectSlug, MergeRequest mr, boolean useLlm) {
       // 1. Fetch files for this PR
       List<ChangedFile> files = fetchFilesSafe(projectSlug, mr.getExternalId());

       // 2. Build temporary MR with files for scoring
       MergeRequest mrWithFiles = enrichWithFiles(mr, files);

       // 3. Score via ScoringEngine
       LlmAssessment llm = useLlm ? llmAnalyzer.analyze(mr) : LlmAssessment.none();
       AnalysisResult result = scoringEngine.evaluate(mrWithFiles, rules, llm);

       // 4. Detect BDD/SDD
       boolean hasBdd = artifactDetector.hasBdd(files);
       boolean hasSdd = artifactDetector.hasSdd(files);

       return new PrAnalysisRow(
           mr.getExternalId(), mr.getTitle(), mr.getAuthor(), mr.getState(), mr.getUrl(),
           mr.getCreatedAt(), mr.getMergedAt(),
           mr.getDiffStats().additions(), mr.getDiffStats().deletions(),
           result.getScore(), result.getVerdict(), result.getRuleResults(),
           result.getLlmComment(),
           hasBdd, hasSdd,
           artifactDetector.findBddFiles(files), artifactDetector.findSddFiles(files));
   }
   ```

   **`buildSummary`** z breakdown:
   ```java
   ProjectSummary buildSummary(List<PrAnalysisRow> rows) {
       int total = rows.size();
       int automatable = count(rows, AUTOMATABLE);
       int maybe = count(rows, MAYBE);
       int notSuitable = count(rows, NOT_SUITABLE);

       double avgScore = rows.stream().mapToDouble(PrAnalysisRow::aiScore).average().orElse(0);

       // Top rules: flatten all ruleResults, group by ruleName, count + avgWeight
       List<RuleFrequency> topRules = rows.stream()
           .flatMap(r -> r.ruleResults().stream())
           .collect(groupingBy(RuleResult::ruleName))
           .entrySet().stream()
           .map(e -> new RuleFrequency(e.getKey(), e.getValue().size(),
               e.getValue().stream().mapToDouble(RuleResult::weight).average().orElse(0)))
           .sorted(comparingInt(RuleFrequency::matchCount).reversed())
           .limit(10)
           .toList();

       // Histogram: 5 buckets [0-0.2, 0.2-0.4, 0.4-0.6, 0.6-0.8, 0.8-1.0]
       List<ScoreHistogramBucket> histogram = buildHistogram(rows);

       int bddCount = (int) rows.stream().filter(PrAnalysisRow::hasBdd).count();
       int sddCount = (int) rows.stream().filter(PrAnalysisRow::hasSdd).count();

       return new ProjectSummary(total,
           automatable, maybe, notSuitable,
           pct(automatable, total), pct(maybe, total), pct(notSuitable, total),
           avgScore, topRules, histogram,
           bddCount, pct(bddCount, total),
           sddCount, pct(sddCount, total));
   }
   ```

### Warstwa 4: Port

9. **`ProjectAnalysisUseCase`** (port in):
   ```java
   public interface ProjectAnalysisUseCase {
       ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm);
   }
   ```

10. **`MergeRequestProvider`** — nowa metoda:
    ```java
    List<ChangedFile> fetchFiles(String projectSlug, String mrId);
    ```

### Warstwa 5: Adapter — config + GitHub + REST

11. **`GitHubAdapter.fetchFiles()`** — delegacja do `GitHubClient.fetchFiles()`, mapping `GitHubFile` → `ChangedFile`

12. **Wzorce w `application.yml`**:
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

13. **`ProjectConfig`** — @Configuration: bean `ArtifactDetector`, `ProjectAnalysisService`

14. **`ProjectAnalysisController`**:
    - `POST /api/project/{owner}/{repo}/analyze?useLlm=false` → `ProjectAnalysisResponse`

15. **`ProjectAnalysisResponse`** — DTO:
    ```json
    {
      "projectSlug": "owner/repo",
      "analyzedAt": "...",
      "summary": {
        "totalPrs": 50,
        "automatableCount": 30, "automatablePercent": 60.0,
        "maybeCount": 12, "maybePercent": 24.0,
        "notSuitableCount": 8, "notSuitablePercent": 16.0,
        "avgScore": 0.62,
        "topRules": [
          { "ruleName": "boost:hasTests", "matchCount": 35, "avgWeight": 0.15 },
          { "ruleName": "penalize:largeDiff", "matchCount": 12, "avgWeight": -0.2 }
        ],
        "histogram": [
          { "rangeStart": 0.0, "rangeEnd": 0.2, "count": 5 },
          { "rangeStart": 0.2, "rangeEnd": 0.4, "count": 3 },
          ...
        ],
        "bddCount": 18, "bddPercent": 36.0,
        "sddCount": 10, "sddPercent": 20.0
      },
      "rows": [
        {
          "prId": "42", "title": "Add login feature", "author": "alice",
          "state": "merged", "url": "https://...",
          "createdAt": "...", "mergedAt": "...",
          "additions": 200, "deletions": 50,
          "aiScore": 0.85, "aiVerdict": "AUTOMATABLE",
          "ruleResults": [
            { "ruleName": "boost:hasTests", "matched": true, "weight": 0.15, "reason": "PR contains test files" },
            { "ruleName": "boost:byKeywords", "matched": true, "weight": 0.1, "reason": "Title contains 'fix'" }
          ],
          "llmComment": null,
          "hasBdd": true, "hasSdd": true,
          "bddFiles": ["src/test/resources/features/login.feature"],
          "sddFiles": ["specs/005-login/spec.md", "specs/005-login/plan.md"]
        }
      ]
    }
    ```

### Warstwa 6: Frontend

16. **Route**: `/project` i `/project/:owner/:repo` + link "Analiza projektu" w navbar

17. **`ProjectAnalysisPage.tsx`**:
    - Repo selector (reużycie)
    - Przycisk "Analizuj projekt" + checkbox "Użyj LLM (kosztowne)"
    - Loading: spinner + "Analizuję N PR-ów..."
    - Po załadowaniu: **3 karty** + **tabela z drill-down**

18. **Karta AI Potential**:
    - Duża: donut chart (AUTOMATABLE green / MAYBE yellow / NOT_SUITABLE red) z % w środku
    - Średni score: "0.62 / 1.0"
    - Counts: "30 automatable, 12 maybe, 8 not suitable"
    - Top rules: mini tabela (ruleName, count, avg weight) — top 5
    - Histogram: SVG bar chart (5 bucketów)

19. **Karta BDD**:
    - Procent: "36% PR-ów z BDD" (duża liczba)
    - Count: "18 z 50"
    - Badge: kolor zależy od %: >50% green, 20-50% yellow, <20% red

20. **Karta SDD**:
    - Analogicznie do BDD: "20% PR-ów z SDD", "10 z 50"

21. **Tabela z drill-down**:
    - Kolumny: PR title, author, date, AI score (badge), Verdict (badge), BDD (✓/✗), SDD (✓/✗)
    - Sortowanie: klik nagłówek → asc/desc toggle
    - Filtry: dropdown Verdict (ALL/AUTOMATABLE/MAYBE/NOT_SUITABLE), BDD (all/tak/nie), SDD (all/tak/nie)
    - **Expandable row**: klik na wiersz → rozwija panel pod wierszem:
      - **Score breakdown**: tabela ruleResults (nazwa, typ boost/penalize, waga, reason)
      - **BDD files**: lista plików (ścieżki) lub "Brak plików BDD"
      - **SDD files**: lista plików (ścieżki) lub "Brak plików SDD"
      - **PR details**: branch, rozmiar (additions/deletions), labels

### Warstwa 7: BDD + testy

22. **`project-analysis.feature`** — scenariusze BDD
23. **`ArtifactDetectorTest`** — unit testy wzorców
24. **`ProjectAnalysisServiceTest`** — mock providers, orchestration, summary building
25. **Frontend tests** — karty, tabela, drill-down, filtry

## Kolejność implementacji

```
Phase 1 — BDD feature file
Phase 2 — Domain models (records)
Phase 3 — ArtifactDetector + tests
Phase 4 — ProjectAnalysisService + tests
Phase 5 — Port + adapter (fetchFiles, config, REST)
Phase 6 — Frontend (page, karty, tabela, drill-down)
Phase 7 — BDD steps + integration + polish
```

## Ryzyka

| Ryzyko | Impact | Mitigation |
|--------|--------|------------|
| 200 PR × fetchFiles = 200 API calls | Wysoki | Parallel (8 threads), activity cache eliminuje detail fetch |
| LLM per PR × 200 = $$ | Wysoki | LLM domyślnie OFF, checkbox + warning |
| Duży JSON response (200 PRs × ruleResults) | Średni | ruleResults per PR to ~5-10 rules, total ~50KB — akceptowalne |
| Timeout na dużym repo | Średni | 5 min timeout, async jako future feature |
