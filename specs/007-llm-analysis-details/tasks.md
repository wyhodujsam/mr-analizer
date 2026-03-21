# 007: Tasks

## BDD Scenarios (pisane PRZED implementacją)

- [ ] T0: Feature file `detailed_llm_analysis.feature` — scenariusze Gherkin

## Backend — Domain

- [ ] T1: Nowe rekordy: `AnalysisCategory`, `HumanOversightItem`, `SummaryAspect`
- [ ] T2: Rozbudowa `LlmAssessment` — nowe pola + factory method backward-compatible
- [ ] T3: Rozbudowa `AnalysisResult` + Builder — nowe pola z LLM
- [ ] T4: Nowy prompt template w `PromptBuilder` — instrukcje dla strukturyzowanego JSON
- [ ] T5: `ScoringEngine` — przepisanie pól z LlmAssessment do AnalysisResult

## Backend — Adaptery

- [ ] T6: `ClaudeCliAdapter` — parsowanie rozbudowanego JSON
- [ ] T7: `AnalysisResultEntity` — nowe kolumny JSON
- [ ] T8: `JpaAnalysisResultRepository` — mapping nowych pól
- [ ] T9: `MrDetailResponse` + `AnalysisResponse.ResultItem` — nowe pola DTO
- [ ] T10: `AnalysisRestController` — mapping w response

## Frontend

- [ ] T11: Typy TypeScript — nowe interfejsy
- [ ] T12: `AnalysisDetailPage` — nowa strona
- [ ] T13: Route w `App.tsx` + nawigacja (przyciski "Szczegóły analizy")

## Testy

- [ ] T14: Step definitions dla BDD
- [ ] T15: Unit testy — LlmAssessment parsing, ScoringEngine, PromptBuilder
