# Tasks: Przegladanie MR/PR i zarzadzanie analizami

**Input**: Design documents from `/specs/002-mr-browse-analyze/`
**Prerequisites**: plan.md, spec.md

## Model Selection Analysis

| Task | Description | Recommended Model | Rationale |
|------|-------------|-------------------|-----------|
| T001 | SavedRepository domain model | Haiku | Simple POJO |
| T002 | New port interfaces | Sonnet | Interface design |
| T003 | SavedRepository JPA entity + Spring Data | Haiku | Standard JPA |
| T004 | JpaSavedRepositoryAdapter | Sonnet | Port impl, entity mapping |
| T005 | ManageReposService | Sonnet | CRUD orchestration |
| T006 | BrowseMrService | Sonnet | Delegates to provider |
| T007 | AnalyzeMrService changes (cache) | Opus | Existing code modification, cache logic |
| T008 | RepoRestController | Sonnet | Standard CRUD REST |
| T009 | BrowseRestController | Sonnet | Simple delegation |
| T010 | AnalysisRestController changes (DELETE, cache) | Opus | Modify existing controller |
| T011 | browse-repos.feature (BDD) | Sonnet | Gherkin scenarios |
| T012 | analysis-cache.feature (BDD) | Sonnet | Gherkin scenarios |
| T013 | BDD step definitions | Opus | Spring context, mock setup |
| T014 | ManageReposServiceTest | Sonnet | Standard unit tests |
| T015 | BrowseMrServiceTest | Sonnet | Standard unit tests |
| T016 | AnalyzeMrService cache test | Opus | Modify existing test |
| T017 | TypeScript types update | Haiku | Add new interfaces |
| T018 | API client update | Sonnet | New functions |
| T019 | RepoSelector component | Opus | Combo dropdown+input, delete, state |
| T020 | MrBrowseTable component | Sonnet | Table without scoring |
| T021 | AnalysisHistory component | Sonnet | List with delete button |
| T022 | DashboardPage rewrite | Opus | Two-step flow, state machine |
| T023 | MrDetailPage update | Sonnet | Handle no-analysis state |
| T024 | app.css updates | Haiku | New styles |
| T025 | Integration test | Opus | Full flow test |

## Phase 1: Backend — Domain & Ports

**Purpose**: New domain model, ports, persistence for saved repos

- [ ] T001 [P] Create SavedRepository domain model in `backend/src/main/java/com/mranalizer/domain/model/SavedRepository.java` — fields: Long id, String projectSlug, String provider, LocalDateTime addedAt, LocalDateTime lastAnalyzedAt (nullable)
- [ ] T002 [P] Create port interfaces: `domain/port/in/BrowseMrUseCase.java` (fetchMergeRequests(FetchCriteria): List<MergeRequest>), `domain/port/in/ManageReposUseCase.java` (getAll, add, delete, findBySlug), `domain/port/out/SavedRepositoryPort.java` (save, findAll, findBySlug, deleteById)
- [ ] T003 [P] Create JPA entity `adapter/out/persistence/entity/SavedRepositoryEntity.java` (@Entity, fields mirror domain) and `SpringDataSavedRepoRepository.java` (extends JpaRepository)
- [ ] T004 Create JpaSavedRepositoryAdapter in `adapter/out/persistence/JpaSavedRepositoryAdapter.java` — implements SavedRepositoryPort, maps entity↔domain, @Component @Profile("!test"). Also create InMemorySavedRepositoryAdapter with @Profile("test") for tests (ConcurrentHashMap-based stub)
- [ ] T005 Create ManageReposService in `application/ManageReposService.java` — implements ManageReposUseCase, auto-adds repo on first browse/analyze
- [ ] T006 Create BrowseMrService in `application/BrowseMrService.java` — implements BrowseMrUseCase, delegates to MergeRequestProvider, saves repo via ManageReposUseCase
- [ ] T007 Modify AnalyzeMrService — add cache detection: before analyzing, check if AnalysisReport exists for same projectSlug+provider; if yes, return cached; add deleteAnalysis(Long reportId) method

---

## Phase 2: Backend — REST API

**Purpose**: New endpoints and modifications to existing controller

- [ ] T008 [P] Create RepoRestController in `adapter/in/rest/RepoRestController.java` — GET /api/repos, POST /api/repos (body: projectSlug, provider), DELETE /api/repos/{id}
- [ ] T009 [P] Create BrowseRestController in `adapter/in/rest/BrowseRestController.java` — POST /api/browse (body: FetchCriteria-like DTO, returns List<MergeRequest> as JSON without scoring)
- [ ] T010 Modify AnalysisRestController — add DELETE /api/analysis/{reportId}, modify POST /api/analysis to check cache first, add query param GET /api/analysis?projectSlug=X to find existing analysis

---

## Phase 3: BDD Tests (test-first)

> **NOTE: Write tests FIRST**

- [ ] T011 [P] Create `backend/src/test/resources/features/browse-repos.feature` — scenarios: browse MR list without scoring, add repo to saved list, saved repos appear in list after refresh, delete repo from saved list, select saved repo loads its slug
- [ ] T012 [P] Create `backend/src/test/resources/features/analysis-cache.feature` — scenarios: analysis results are cached, cached analysis loads instantly, delete analysis allows re-analysis, analysis history shows all past analyses
- [ ] T013 Create step definitions: `bdd/steps/BrowseRepoSteps.java` and `bdd/steps/AnalysisCacheSteps.java` — mock provider, use TestRestTemplate

---

## Phase 4: Unit Tests

- [ ] T014 [P] Create ManageReposServiceTest — tests: add repo, get all, delete, find by slug, duplicate slug not added twice
- [ ] T015 [P] Create BrowseMrServiceTest — tests: delegates to provider, saves repo on browse
- [ ] T016 Modify AnalyzeMrServiceTest — add tests: returns cached when exists, analyzes fresh when no cache, deleteAnalysis removes from repo

---

## Phase 5: Frontend

- [ ] T017 [P] Update `frontend/src/types/index.ts` — add: SavedRepository, BrowseResponse (list of MR without scoring), MrBrowseItem
- [ ] T018 [P] Update `frontend/src/api/analysisApi.ts` — add: getRepos(), addRepo(), deleteRepo(), browseMrs(), deleteAnalysis(), getAnalysisBySlug()
- [ ] T019 Create RepoSelector component in `frontend/src/components/RepoSelector.tsx` — combo: dropdown of saved repos + text input for new slug + "X" delete button per saved repo; on select fills slug; on new slug submit adds to saved
- [ ] T020 [P] Create MrBrowseTable component in `frontend/src/components/MrBrowseTable.tsx` — table: #, title, author, created date, status, files count; click opens MR detail in new tab (target="_blank"); NO scoring columns
- [ ] T021 [P] Create AnalysisHistory component in `frontend/src/components/AnalysisHistory.tsx` — list of past analyses: date, repo, MR count, verdict summary, "Usun" button with confirm dialog
- [ ] T022 Rewrite DashboardPage in `frontend/src/pages/DashboardPage.tsx` — two-step flow: Step 1 (RepoSelector + date range + "Pobierz MR" → MrBrowseTable), Step 2 ("Analizuj" button → SummaryCard + MrTable with scores); show AnalysisHistory below; detect cached analysis
- [ ] T023 Modify MrDetailPage in `frontend/src/pages/MrDetailPage.tsx` — handle state where MR has no analysis: show MR data (title, description rendered as markdown via react-markdown, author, branches, dates, labels, diff stats, changed files list) WITHOUT scoring section; show scoring section only if analysis exists; open in new tab support (target="_blank" on MrBrowseTable rows); add react-markdown to package.json
- [ ] T024 [P] Update `frontend/src/styles/app.css` — styles for RepoSelector dropdown, MrBrowseTable, AnalysisHistory, two-step flow visual separation

---

## Phase 6: Polish & Integration

- [ ] T025 Create integration test for full flow — browse → analyze → cache hit → delete → re-analyze; saved repos CRUD
- [ ] T026 Run all tests, verify all pass (existing 60 + new BDD + new unit)

---

## Dependencies & Execution Order

- **Phase 1**: Domain + ports + persistence — no deps, start immediately
- **Phase 2**: REST API — depends on Phase 1
- **Phase 3**: BDD tests — can start parallel with Phase 2 (test-first)
- **Phase 4**: Unit tests — can start parallel with Phase 2
- **Phase 5**: Frontend — depends on Phase 2 (needs API endpoints)
- **Phase 6**: Integration — depends on all phases

## Implementation Strategy

1. Phase 1: Backend domain (SavedRepository, ports, persistence)
2. Phase 2+3+4 parallel: REST API + BDD tests + unit tests
3. Phase 5: Frontend (two-step flow, RepoSelector, AnalysisHistory)
4. Phase 6: Integration test + final verification
