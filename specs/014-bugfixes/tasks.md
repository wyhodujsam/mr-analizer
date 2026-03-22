# Tasks: 014 — Bugfixes

## Phase 1: BDD Scenarios (test-first)

- [x] T001 [US1] Dodac BDD scenariusz: browse pobiera MR bez osobnych wywolan na pliki — `provider.feature`
- [x] T002 [US2] Dodac BDD scenariusz: PR z 1 plikiem + pozytywny LLM → score > 0 (soft exclude) — `scoring.feature`
- [x] T003 [US2] Zweryfikowac ze istniejacy scenariusz "hotfix label is excluded" nadal przechodzi (hard exclude)

## Phase 2: BUG-001 — Lazy loading plikow

- [x] T004 [US1] Dodac pola `additions`, `deletions`, `changed_files` do `GitHubPullRequest.java`
- [x] T005 [US1] Dodac metode `toDomainWithoutFiles(pr, projectSlug)` w `GitHubMapper.java` — diffStats z PR response, changedFiles=empty
- [x] T006 [US1] Zmienic `GitHubAdapter.fetchMergeRequests()` — usunac fetchFiles loop, uzyc toDomainWithoutFiles
- [x] T007 [US1] Zaktualizowac `GitHubAdapterTest.java` i `GitHubMapperTest.java`

## Phase 3: BUG-003 — Soft exclude

- [x] T008 [US2] Dodac `SOFT_EXCLUDE_WEIGHT = -0.4` w `ScoringEngine.java`
- [x] T009 [US2] Zmienic `ExcludeRule` factory methods: byMinChangedFiles, byMaxChangedFiles, byFileExtensionsOnly → uzywaja SOFT_EXCLUDE_WEIGHT
- [x] T010 [US2] Zmienic logike `ScoringEngine.evaluate()` — only EXCLUDE_WEIGHT triggers hard exclude, SOFT_EXCLUDE_WEIGHT traktowany jak penalize
- [x] T011 [US2] Zaktualizowac unit testy `ScoringEngineTest.java` i `ExcludeRuleTest.java`

## Phase 4: Polish

- [x] T012 Uruchomienie wszystkich testow (`mvn test`)
- [x] T013 Oznaczenie BUG-001 i BUG-003 jako naprawione w plikach bugs/
