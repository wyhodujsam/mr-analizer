# Implementation Plan: MVP Core

**Branch**: `001-mvp-core` | **Date**: 2026-03-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-mvp-core/spec.md`

## Summary

Zbudowac kompletne MVP aplikacji MR Analizer: backend Spring Boot (hexagonal) z REST API do analizy Pull Requestow z GitHub, silnik regul (exclude/boost/penalize) ze scoringiem, adapter Claude CLI do opcjonalnej analizy LLM, persystencja H2, oraz frontend React+TypeScript z dashboardem, formularzem analizy i tabela wynikow. Testy BDD (Cucumber/Gherkin) + jednostkowe.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Validation, WebFlux client), React 18, Vite, React-Bootstrap, Axios, React Router, Lombok
**Storage**: H2 (dev/test), Spring Data JPA
**Testing**: JUnit 5, Mockito, Cucumber 7 (cucumber-java, cucumber-junit-platform-engine, cucumber-spring), Spring Test
**Target Platform**: Linux server (self-hosted, Tailscale)
**Project Type**: Web application (backend REST API + frontend SPA)
**Performance Goals**: Analiza 100 PR w < 60s (bez LLM), start aplikacji < 30s
**Constraints**: GitHub API rate limit 5000 req/h, Claude CLI timeout 60s per PR
**Scale/Scope**: Single user, 1-5 repozytoriow, do 1000 PR per analiza

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | domain/ (pure logic, no Spring imports), application/ (use cases), adapter/in/rest/, adapter/out/provider|llm|persistence |
| II. Provider Abstraction | PASS | MergeRequestProvider interface (GitHub adapter), LlmAnalyzer interface (ClaudeCli + NoOp adapters), runtime config via application.yml |
| III. BDD Testing | PASS | .feature files before implementation, Cucumber 7 + JUnit 5, scenarios map to spec.md acceptance criteria |
| IV. SDD Workflow | PASS | spec.md → plan.md → tasks.md → implement |
| V. Simplicity (YAGNI) | PASS | Only MVP scope, no GitLab adapter, no charts, no filters — those are Faza 2+ |

## Project Structure

### Documentation (this feature)

```text
specs/001-mvp-core/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── rest-api.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/mranalizer/
    │   │   ├── MrAnalizerApplication.java
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── MergeRequest.java
    │   │   │   │   ├── ChangedFile.java
    │   │   │   │   ├── DiffStats.java
    │   │   │   │   ├── AnalysisResult.java
    │   │   │   │   ├── AnalysisReport.java
    │   │   │   │   ├── Verdict.java
    │   │   │   │   ├── FetchCriteria.java
    │   │   │   │   └── LlmAssessment.java
    │   │   │   ├── rules/
    │   │   │   │   ├── Rule.java
    │   │   │   │   ├── RuleResult.java
    │   │   │   │   ├── ExcludeRule.java
    │   │   │   │   ├── BoostRule.java
    │   │   │   │   └── PenalizeRule.java
    │   │   │   ├── scoring/
    │   │   │   │   ├── ScoringEngine.java
    │   │   │   │   └── ScoringConfig.java
    │   │   │   └── port/
    │   │   │       ├── in/
    │   │   │       │   ├── AnalyzeMrUseCase.java
    │   │   │       │   └── GetAnalysisResultsUseCase.java
    │   │   │       └── out/
    │   │   │           ├── MergeRequestProvider.java
    │   │   │           ├── LlmAnalyzer.java
    │   │   │           └── AnalysisResultRepository.java
    │   │   ├── application/
    │   │   │   ├── AnalyzeMrService.java
    │   │   │   └── dto/
    │   │   │       ├── AnalysisRequestDto.java
    │   │   │       └── AnalysisSummaryDto.java
    │   │   └── adapter/
    │   │       ├── in/rest/
    │   │       │   ├── AnalysisRestController.java
    │   │       │   └── dto/
    │   │       │       ├── AnalysisResponse.java
    │   │       │       ├── MrDetailResponse.java
    │   │       │       └── ErrorResponse.java
    │   │       ├── out/
    │   │       │   ├── provider/github/
    │   │       │   │   ├── GitHubAdapter.java
    │   │       │   │   ├── GitHubClient.java
    │   │       │   │   ├── GitHubMapper.java
    │   │       │   │   └── dto/
    │   │       │   │       ├── GitHubPullRequest.java
    │   │       │   │       └── GitHubFile.java
    │   │       │   ├── llm/
    │   │       │   │   ├── ClaudeCliAdapter.java
    │   │       │   │   └── NoOpLlmAdapter.java
    │   │       │   └── persistence/
    │   │       │       ├── JpaAnalysisResultRepository.java
    │   │       │       ├── SpringDataAnalysisResultRepository.java
    │   │       │       └── entity/
    │   │       │           ├── AnalysisResultEntity.java
    │   │       │           └── MergeRequestEntity.java
    │   │       └── config/
    │   │           ├── ProviderConfig.java
    │   │           ├── LlmConfig.java
    │   │           ├── CorsConfig.java
    │   │           └── RulesConfig.java
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/mranalizer/
        │   ├── domain/
        │   │   ├── scoring/ScoringEngineTest.java
        │   │   └── rules/
        │   │       ├── ExcludeRuleTest.java
        │   │       ├── BoostRuleTest.java
        │   │       └── PenalizeRuleTest.java
        │   ├── application/
        │   │   └── AnalyzeMrServiceTest.java
        │   └── bdd/
        │       ├── CucumberTestRunner.java
        │       ├── CucumberSpringConfig.java
        │       └── steps/
        │           ├── AnalysisSteps.java
        │           ├── ScoringSteps.java
        │           └── ProviderSteps.java
        └── resources/
            ├── features/
            │   ├── analysis.feature
            │   ├── scoring.feature
            │   └── provider.feature
            └── application-test.yml

frontend/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/
    │   └── analysisApi.ts
    ├── types/
    │   └── index.ts
    ├── components/
    │   ├── Layout.tsx
    │   ├── AnalysisForm.tsx
    │   ├── MrTable.tsx
    │   ├── ScoreBadge.tsx
    │   └── SummaryCard.tsx
    ├── pages/
    │   ├── DashboardPage.tsx
    │   └── MrDetailPage.tsx
    └── styles/
        └── app.css
```

**Structure Decision**: Web application — backend (Spring Boot REST API) + frontend (React SPA). Backend follows hexagonal architecture with strict package separation. Frontend is a standard Vite React project.

## Complexity Tracking

No constitution violations. No complexity justification needed.
