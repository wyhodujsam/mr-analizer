# Implementation Plan: 014 — Bugfixes

**Branch**: `014-bugfixes` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)

## Summary

Dwa bugfixy: (1) lazy loading plikow w browse — usunac fetchFiles z fetchMergeRequests, dodac pola additions/deletions/changed_files do GitHubPullRequest DTO, (2) soft exclude — rozdzielic ExcludeRule na hard (labels) i soft (files) z konfigurowalna kara.

## Constitution Check

| Gate | Status |
|------|--------|
| I. Hexagonal | PASS — BUG-001 zmiana w adapter, BUG-003 zmiana w domain |
| II. Provider Abstraction | PASS — zmiany w GitHubAdapter nie wplywaja na port interface |
| III. BDD | PASS — nowe scenariusze + aktualizacja istniejacych |
| IV. SDD | PASS |
| V. YAGNI | PASS — minimalne zmiany |

## Design: BUG-001

1. Dodac pola `additions`, `deletions`, `changed_files` do `GitHubPullRequest` DTO (GitHub API zwraca je w PR list response)
2. Dodac druga metode mapowania w `GitHubMapper`: `toDomainWithoutFiles(pr, projectSlug)` — uzywa pól z PR response zamiast fetchFiles
3. W `GitHubAdapter.fetchMergeRequests()` — usunac fetchFiles, uzyc `toDomainWithoutFiles`
4. `fetchMergeRequest()` — bez zmian (nadal fetchuje pliki dla analizy per-MR)

## Design: BUG-003

1. Rozdzielic `ExcludeRule` na dwa typy: `EXCLUDE_WEIGHT` (hard, labels) i nowy `SOFT_EXCLUDE_WEIGHT` (-0.4, files)
2. W `ScoringEngine.evaluate()` — hard exclude dziala jak dotychczas. Soft exclude traktowany jak zwykla regula penalize (dodany do score, nie zeruje)
3. W `ExcludeRule` factory methods: `byLabels` → hard exclude (EXCLUDE_WEIGHT), `byMinChangedFiles/byMaxChangedFiles/byFileExtensionsOnly` → soft exclude (SOFT_EXCLUDE_WEIGHT)

## Zmiany w plikach

### BUG-001
| Plik | Zmiana |
|------|--------|
| `GitHubPullRequest.java` | +3 pola: additions, deletions, changed_files |
| `GitHubMapper.java` | +metoda toDomainWithoutFiles() |
| `GitHubAdapter.java` | fetchMergeRequests() uzywa toDomainWithoutFiles zamiast fetchFiles |
| `GitHubAdapterTest.java` | aktualizacja testow |
| `provider.feature` | nowy scenariusz BDD |

### BUG-003
| Plik | Zmiana |
|------|--------|
| `ScoringEngine.java` | +SOFT_EXCLUDE_WEIGHT, zmieniona logika excluded branch |
| `ExcludeRule.java` | factory methods file-based uzywaja SOFT_EXCLUDE_WEIGHT |
| `ScoringEngineTest.java` | testy soft exclude |
| `scoring.feature` | nowy scenariusz BDD + aktualizacja istniejacych |
