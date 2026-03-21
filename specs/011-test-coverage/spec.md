# 011 — Test Coverage 95%+

## Cel
Pokrycie testami >= 95% w kazdym pakiecie/komponencie (backend + frontend).

## Obszary do pokrycia

### Backend
1. adapter.out.provider.github (6% → 95%+) — GitHubClient, GitHubAdapter, GitHubMapper z MockWebServer
2. adapter.out.llm (2% → 95%+) — ClaudeCliAdapter z mockowanym procesem
3. adapter.out.persistence (26% → 95%+) — JpaAnalysisResultRepository, entity mapping
4. adapter.in.rest (64% → 95%+) — controllery, exception handler
5. domain.exception (46% → 95%+) — exception construction

### Frontend
6. Brakujace testy: DashboardPage, MrTable, MrBrowseTable, RepoSelector, SummaryCard, VerdictPieChart
7. Niskie pokrycie: AnalysisHistory (69%), verdict.ts (40%)
