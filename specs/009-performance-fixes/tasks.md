# 009 — Performance Fixes: Tasks

### T1: Paginacja GitHub z limitem
**Pliki:** GitHubClient.java, GitHubAdapter.java
**AC:** US-1

### T2: Rownolegle wywolania LLM
**Pliki:** AnalyzeMrService.java
**AC:** US-2

### T3: Dedykowane query — findResult
**Pliki:** SpringDataAnalysisResultRepository, JpaAnalysisResultRepository, AnalysisResultRepository port, GetAnalysisResultsService
**AC:** US-3 (AC-3.1)

### T4: Dedykowane query — findByProjectSlug
**Pliki:** SpringDataAnalysisResultRepository, JpaAnalysisResultRepository, AnalysisResultRepository port, AnalysisRestController
**AC:** US-3 (AC-3.2)

## Kolejnosc
T1-T4 niezalezne, mozna rownolegle.
