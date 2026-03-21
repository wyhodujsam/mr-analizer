# Implementation Plan: Analiza per MR + konfigurowalny prompt LLM

**Branch**: `004-analysis-per-mr-llm-config` | **Date**: 2026-03-21 | **Spec**: [spec.md](spec.md)

## Summary

Dwie zmiany: (1) Usuniecie cache per repo — kazda analiza tworzy nowy raport, nie blokuje kolejnych. (2) Wydzielenie promptu LLM do konfiguracji w application.yml z placeholderami i konfigurowalnym szablonem odpowiedzi.

## Technical Context

**Zmiany w istniejacym kodzie:**
- `AnalyzeMrService.analyze()` — usunac cache detection (`findByProjectSlug`)
- `AnalysisResultRepository` — usunac `findByProjectSlug`
- `ClaudeCliAdapter` — wydzielic `buildPrompt()` do konfigurowalnego PromptBuilder
- `application.yml` — dodac sekcje `mr-analizer.llm.claude-cli.prompt` i `response-template`
- Frontend `DashboardPage` — usunac cache detection (getAnalysisBySlug badge)

**Nowe klasy:**
- `PromptBuilder` (domain) — buduje prompt z template + MergeRequest data
- `LlmPromptConfig` (adapter/config) — laduje konfiguracje z YAML

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal | PASS | PromptBuilder pure domain, config w adapter |
| II. Provider Abstraction | PASS | LlmAnalyzer port bez zmian |
| III. BDD | PASS | Nowe scenariusze |
| IV. SDD | PASS | spec → plan → tasks |
| V. Simplicity | PASS | Minimalne zmiany |
