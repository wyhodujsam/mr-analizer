# Tasks: 013 — Profile Recommendations

## Phase 1: BDD Scenarios (test-first)

- [x] T001 [US1] Dodac BDD scenariusz: pobieranie raportu analizy laduje wyniki w ograniczonej liczbie zapytan SQL — `backend/src/test/resources/features/analysis.feature`
- [x] T002 [US2] Dodac BDD scenariusz: browse MR z brakujacym tokenem GitHub zwraca blad autentykacji natychmiast — `backend/src/test/resources/features/provider.feature`

## Phase 2: US-1 — N+1 Fix (EntityGraph)

- [x] T003 [US1] Dodac `@EntityGraph(attributePaths = {"results"})` na nowej metodzie `findByIdWithResults` w `SpringDataAnalysisResultRepository.java`
- [x] T004 [US1] Uzyc `findByIdWithResults` w `JpaAnalysisResultRepository.findById()` zamiast domyslnego `findById`

## Phase 3: US-2 — Early Token Validation

- [x] T005 [US2] Dodac flage `tokenConfigured` w `GitHubClient` konstruktorze (true jesli token != null && !blank)
- [x] T006 [US2] Dodac walidacje na poczatku `fetchPullRequests`, `fetchPullRequest`, `fetchFiles` — jesli !tokenConfigured rzucic `ProviderAuthException("GitHub token is not configured")`
- [x] T007 [US2] Unit test `GitHubClientTest` — sprawdzic ze bez tokena rzuca ProviderAuthException natychmiast

## Phase 4: US-3 — Per-endpoint HTTP Metrics

- [x] T008 [US3] W `scripts/profile.sh` zamienic sekcje HTTP Latency z raw JSON na per-endpoint tabele (iteracja po URI tagach, curl per tag, formatowanie jako Markdown table)

## Phase 5: Polish

- [x] T009 Uruchomienie wszystkich testow (`mvn test`)
- [x] T010 Zaktualizowac tasks.md — zaznaczyc ukonczone taski
