# 009 — Performance Fixes: Plan

## P1: Paginacja GitHub z limitem
- GitHubClient.fetchPullRequests — przyjmuje `limit` i przerywa paginacje po zebraniu wystarczajacej liczby PR
- per_page = min(limit, 100) — GitHub API max to 100
- Po kazdej stronie sprawdzenie czy allPrs.size() >= limit, jesli tak — break

## P2: Rownolegle wywolania LLM
- AnalyzeMrService — uzyc CompletableFuture + ExecutorService(3 threads)
- Kazde wywolanie LLM w osobnym wasku z timeoutem
- Blad jednego MR nie blokuje pozostalych (juz jest — fallback w ClaudeCliAdapter)
- ExecutorService jako bean w LlmConfig lub field z @PreDestroy

## P3: Dedykowane query do bazy
- SpringDataAnalysisResultRepository — dodac findByReportIdAndResultId (native query lub derived)
- AnalysisResultRepository port — dodac Optional<AnalysisResult> findResult(Long reportId, Long resultId)
- GetAnalysisResultsService — uzyc nowego query zamiast ladowania calego raportu
- SpringDataAnalysisResultRepository — dodac findByProjectSlug dla raportow
- AnalysisResultRepository port — dodac List<AnalysisReport> findByProjectSlug(String slug)
- AnalysisRestController — uzyc nowego query
