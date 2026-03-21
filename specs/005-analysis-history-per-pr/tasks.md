# Tasks: Historia analiz per PR z filtrowaniem

**Input**: Design documents from `/specs/005-analysis-history-per-pr/`

## Model Selection Analysis

| Task | Description | Recommended Model | Rationale |
|------|-------------|-------------------|-----------|
| T001 | BDD scenarios | Sonnet | Gherkin |
| T002 | Backend: return results in list endpoint | Sonnet | Simple controller change |
| T003 | Frontend: rewrite AnalysisHistory | Opus | Complex UI, filter, per-PR rows |
| T004 | Frontend: DashboardPage filter integration | Sonnet | Wire up props |
| T005 | BDD step definitions | Opus | Assertions |
| T006 | Run tests + verify | Sonnet | Verification |

## Phase 1: BDD Tests (test-first)

- [ ] T001 [US1][US2] Add scenarios to `backend/src/test/resources/features/analysis-cache.feature`:
  - "Analysis history shows individual PRs with title, score and verdict"
  - "Analysis history can be filtered by repository"
  - "Analysis with multiple PRs shows separate rows per PR"
- [ ] T002 [US1][US2] Add step definitions in `AnalysisCacheSteps.java`

## Phase 2: Backend

- [ ] T003 [US1] Modify `AnalysisRestController.listReports()` — return full AnalysisResponse.from(report) with results instead of empty list. This is the only backend change needed.

## Phase 3: Frontend

- [ ] T004 [US1][US2] Rewrite `AnalysisHistory.tsx`:
  - Rows per PR (not per report): #, Title, Author, Score (ScoreBadge), Verdict, Date, Repo, Usun button
  - Flatten: iterate analyses → iterate results → render row per result
  - Click row → window.open(result.url, '_blank')
  - reportId grouped visually (same reportId = same analysis batch)
  - Delete button deletes entire report (not single PR)
- [ ] T005 [US2] Add repo filter dropdown to AnalysisHistory:
  - Extract unique repos from analyses
  - Dropdown: "Wszystkie" + list of repos
  - Filter rows when selected
- [ ] T006 [US1] Update DashboardPage — pass analyses with results to AnalysisHistory (already does via getAnalyses, just ensure results are populated now)

## Phase 4: Wykres kolowy (US3)

- [ ] T007 [US3] Install chart.js + react-chartjs-2 in frontend (`npm install chart.js react-chartjs-2`)
- [ ] T008 [US3] Create `VerdictPieChart.tsx` component — Pie/Doughnut chart: 3 segments (AUTOMATABLE green, MAYBE yellow, NOT_SUITABLE red), labels with counts, shown after analysis
- [ ] T009 [US3] Add VerdictPieChart to DashboardPage — render next to SummaryCard when analysisResponse exists

## Phase 5: Polish

- [ ] T010 Run all tests, verify pass
