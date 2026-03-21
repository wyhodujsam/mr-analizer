# Tasks: Analiza per MR + konfigurowalny prompt LLM

**Input**: Design documents from `/specs/004-analysis-per-mr-llm-config/`

## Model Selection Analysis

| Task | Description | Recommended Model | Rationale |
|------|-------------|-------------------|-----------|
| T001 | Remove cache from AnalyzeMrService | Sonnet | Simple code removal |
| T002 | Remove findByProjectSlug from repo | Sonnet | Interface + impl cleanup |
| T003 | Remove cache UI from frontend | Sonnet | State cleanup |
| T004 | PromptBuilder domain class | Opus | Template parsing, placeholder replacement |
| T005 | LLM prompt config in YAML | Sonnet | Config properties |
| T006 | Refactor ClaudeCliAdapter | Opus | Integrate PromptBuilder, configurable response parsing |
| T007 | BDD scenarios | Sonnet | Gherkin |
| T008 | Step definitions | Opus | Spring context, mock setup |
| T009 | Unit tests | Opus | PromptBuilder + cache removal verification |
| T010 | Integration test update | Sonnet | Fix existing tests |

## Phase 1: Remove cache-per-repo (US1)

### BDD Tests (test-first)

- [ ] T001 [P] [US1] Add scenarios to `backend/src/test/resources/features/analysis-cache.feature`: "Multiple analyses for same repo coexist", "New analysis does not overwrite previous", "Deleting one analysis leaves others intact"
- [ ] T002 [US1] Add step definitions for new scenarios in `AnalysisCacheSteps.java`

### Implementation

- [ ] T003 [US1] Remove cache detection from `AnalyzeMrService.analyze()` — delete lines 49-52 (findByProjectSlug check). Every analyze() call creates a fresh report.
- [ ] T004 [US1] Remove `findByProjectSlug` from `AnalysisResultRepository` port, `JpaAnalysisResultRepository`, `InMemoryAnalysisResultRepository`, `SpringDataAnalysisResultRepository`
- [ ] T005 [US1] Remove `getAnalysisBySlug` from frontend: remove function from `analysisApi.ts`, remove cache badge + handleLoadCached + handleDeleteCached from `DashboardPage.tsx`, remove `cachedAnalysis` state
- [ ] T006 [US1] Fix existing tests that use `findByProjectSlug` or rely on cache behavior — update `AnalyzeMrServiceTest`, `IntegrationTest`, `AnalysisCacheSteps`

**Checkpoint**: Multiple analyses for same repo work independently

---

## Phase 2: Configurable LLM prompt (US2)

### BDD Tests (test-first)

- [ ] T007 [P] [US2] Add scenarios to `backend/src/test/resources/features/analysis.feature`: "LLM receives configured prompt with MR data", "Default prompt used when no custom config"
- [ ] T008 [US2] Add step definitions in `AnalysisSteps.java`

### Implementation

- [ ] T009 [US2] Create `PromptBuilder` in `backend/src/main/java/com/mranalizer/domain/scoring/PromptBuilder.java` — pure domain, takes template string, replaces placeholders: `{{title}}`, `{{description}}`, `{{filesChanged}}`, `{{additions}}`, `{{deletions}}`, `{{hasTests}}`, `{{labels}}`, `{{author}}`, `{{sourceBranch}}`, `{{targetBranch}}`. Unknown placeholders → empty string + log warning.
- [ ] T010 [US2] Update `application.yml` — add configurable prompt and response template:
  ```yaml
  mr-analizer:
    llm:
      claude-cli:
        prompt-template: |
          Analyze this Pull Request for LLM automation potential.
          Rate from -0.5 to +0.5 how suitable it is for automated execution.
          Respond in JSON: {"scoreAdjustment": 0.1, "comment": "explanation"}

          Title: {{title}}
          Description: {{description}}
          Files changed: {{filesChanged}}
          Additions: {{additions}}
          Deletions: {{deletions}}
          Has tests: {{hasTests}}
          Labels: {{labels}}
          Author: {{author}}
          Source branch: {{sourceBranch}}
          Target branch: {{targetBranch}}
        response-score-field: scoreAdjustment
        response-comment-field: comment
  ```
- [ ] T011 [US2] Refactor `ClaudeCliAdapter` — inject PromptBuilder + config values. Replace hardcoded `buildPrompt()` with `promptBuilder.build(template, mr)`. Use configurable field names in `parseResponse()`.
- [ ] T012 [US2] Create PromptBuilderTest — tests: all placeholders replaced, missing fields → empty string, null template → default prompt, custom template works

---

## Phase 3: Polish

- [ ] T013 Run all tests (`mvn test`), fix any failures
- [ ] T014 Update SPEC.md with changes

---

## Dependencies

- Phase 1 (cache removal) and Phase 2 (prompt config) are independent — can run in parallel
- Phase 3 depends on both
