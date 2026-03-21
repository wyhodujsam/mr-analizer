# 008 — Code Review Fixes: Tasks

## Faza 1: Krytyczne (P0)

### T1: NPE safety — MergeRequest defaults + BDD
**Zalezy od:** nic
**Pliki:**
- `domain/model/MergeRequest.java` — defaults w constructor
- `test/resources/features/scoring-null-safety.feature` — nowe scenariusze BDD
- `test/java/.../bdd/steps/ScoringSteps.java` — nowe step definitions
- `domain/rules/ExcludeRuleTest.java` — unit testy null cases
- `domain/rules/BoostRuleTest.java` — unit testy null cases
- `domain/rules/PenalizeRuleTest.java` — unit testy null cases

**AC:** US-1 (AC-1.1 do AC-1.4)

### T2: ClaudeCliAdapter stderr fix
**Zalezy od:** nic
**Pliki:**
- `adapter/out/llm/ClaudeCliAdapter.java` — redirectErrorStream(true)

**AC:** US-2 (AC-2.1, AC-2.2)

### T3: H2 console security
**Zalezy od:** nic
**Pliki:**
- `src/main/resources/application.yml` — h2.console.enabled: false w default, true w profilu dev

**AC:** US-3 (AC-3.1)

### T4: GitHubClient HTTP errors + SSRF + NoSuchElement fix
**Zalezy od:** nic
**Pliki:**
- `adapter/out/provider/github/GitHubClient.java` — onStatus handlery, UriComponentsBuilder, NoSuchElement → ProviderException
- `test/resources/features/github-errors.feature` — BDD scenariusze
- `test/java/.../bdd/steps/ProviderSteps.java` — nowe step definitions

**AC:** US-3 (AC-3.2, AC-3.3), US-5 (AC-5.4)

## Faza 2: Bugi i architektura (P1)

### T5: BrowseMrService cache fix + port extraction
**Zalezy od:** T1 (FetchCriteria.cacheKey uzywa pol ktore moga byc null)
**Pliki:**
- `domain/model/FetchCriteria.java` — cacheKey() method
- `application/BrowseMrService.java` — computeIfAbsent, nowy cache key
- `domain/port/in/BrowseMrUseCase.java` — dodanie invalidateCache/hasCachedResults
- `adapter/in/rest/BrowseRestController.java` — zalezy od BrowseMrUseCase (interfejs)
- `application/BrowseMrServiceTest.java` — testy cache

**AC:** US-4 (AC-4.1 do AC-4.3)

### T6: Walidacja inputu backend
**Zalezy od:** T4 (GitHubClient juz obsluguje bledy HTTP)
**Pliki:**
- `adapter/in/rest/RepoRestController.java` — walidacja addRepo
- `adapter/out/provider/github/GitHubAdapter.java` — try-catch parseInt
- `domain/model/FetchCriteria.java` — validate() method
- `application/dto/AnalysisRequestDto.java` — wywolanie validate()
- `test/resources/features/validation.feature` — BDD scenariusze
- `test/java/.../bdd/steps/ValidationSteps.java` — step definitions

**AC:** US-5 (AC-5.1 do AC-5.3)

### T7: Frontend types + error handling + React fixes
**Zalezy od:** nic (frontend niezalezny od backendu)
**Pliki:**
- `frontend/src/types/index.ts` — nullable types
- `frontend/src/components/ScoreBadge.tsx` — null-safe score
- `frontend/src/components/AnalysisHistory.tsx` — precompute seenReports
- `frontend/src/pages/DashboardPage.tsx` — error states
- `frontend/src/pages/MrDetailPage.tsx` — AbortController
- `frontend/src/pages/AnalysisDetailPage.tsx` — AbortController
- `frontend/src/test/AnalysisDetailPage.test.tsx` — usuniecie as unknown as
- `frontend/src/test/MrDetailPage.test.tsx` — usuniecie as unknown as
- `frontend/src/test/AnalysisHistory.test.tsx` — aktualizacja

**AC:** US-6 (AC-6.1 do AC-6.5)

## Faza 3: Czyszczenie (P2/P3)

### T8: Dead code removal
**Zalezy od:** T5 (ProviderConfig usuwany po potwierdzeniu ze nie jest uzywany)
**Pliki:**
- usunac: `frontend/src/components/AnalysisForm.tsx`
- usunac: `adapter/config/ProviderConfig.java`
- edytowac: `frontend/src/types/index.ts` — usunac AnalysisSummary, ErrorResponse
- edytowac: `frontend/src/api/analysisApi.ts` — usunac getSummary
- edytowac: `backend/pom.xml` — usunac Lombok
- edytowac: `adapter/out/llm/ClaudeCliAdapter.java` — podwojny srednik

**AC:** US-7 (AC-7.1 do AC-7.5)

### T9: DRY frontend utils
**Zalezy od:** T7 (po poprawce typow)
**Pliki:**
- nowy: `frontend/src/utils/format.ts` — formatDate
- nowy: `frontend/src/utils/verdict.ts` — verdictClass, verdictLabel
- nowy: `frontend/src/utils/error.ts` — extractApiError
- edytowac: MrBrowseTable, MrDetailPage, AnalysisHistory, MrTable, DashboardPage — uzyc utilsow

**AC:** US-7 (AC-7.6)

### T10: Drobne poprawki backend
**Zalezy od:** T1 (hasTests wymaga ze testy T1 juz dzialaja)
**Pliki:**
- `domain/model/MergeRequest.java` — isHasTests → hasTests
- `domain/scoring/ScoringEngine.java` — exclude threshold
- `domain/rules/BoostRule.java` — rename byDescriptionKeywords
- `test/java/.../bdd/steps/ScoringSteps.java` — aktualizacja
- `test/java/.../domain/scoring/ScoringEngineTest.java` — aktualizacja

**AC:** review.md #12, #44, #47

---

## Kolejnosc wykonania

```
T1 ──┬──→ T5 ──→ T8
T2 ──┤
T3 ──┤         ┌→ T9
T4 ──┴──→ T6   │
T7 ─────────┴──┤
T1 ──────────→ T10
```

Faza 1 (T1-T4) mozna robic rownolegle.
Faza 2 (T5-T7) po Fazie 1, T7 mozna rowolegle z T5/T6.
Faza 3 (T8-T10) po Fazie 2.
