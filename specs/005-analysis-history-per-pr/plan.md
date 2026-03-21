# Implementation Plan: Historia analiz per PR z filtrowaniem

**Branch**: `005-analysis-history-per-pr` | **Date**: 2026-03-21 | **Spec**: [spec.md](spec.md)

## Summary

Zmiana AnalysisHistory z widoku per-raport (agregaty) na widok per-PR (konkretne MR z score/verdict). Dodanie filtra po repozytorium. Backend: zmiana GET /api/analysis aby zwracal results. Frontend: przebudowa AnalysisHistory.

## Zmiany

### Backend
- `AnalysisRestController.listReports()` — zwracac pełne AnalysisResponse z results (zamiast pustej listy)
- Bez nowych endpointow, bez nowych encji

### Frontend
- `AnalysisHistory.tsx` — przebudowa: wiersze per PR, filtr dropdown po repo, ScoreBadge, klik otwiera nowa karte
- `types/index.ts` — bez zmian (AnalysisResponse juz ma results)
- `DashboardPage.tsx` — przekazac savedRepos do AnalysisHistory jako zrodlo filtra

## Constitution Check

Wszystkie zasady PASS — minimalna zmiana, reuse istniejacych komponentow.
