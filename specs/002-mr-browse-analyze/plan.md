# Implementation Plan: Przegladanie MR/PR i zarzadzanie analizami

**Branch**: `002-mr-browse-analyze` | **Date**: 2026-03-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-mr-browse-analyze/spec.md`

## Summary

Zmiana flow aplikacji z jednoetapowego "analizuj od razu" na dwuetapowy "przegladaj MR → analizuj". Dodanie zapamietywania repozytoriow (dropdown), zapisywania/usuwania analiz, oraz podgladu szczegolow MR w nowej karcie z pelnymi danymi z GitHub API.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.x (existing), React 18 (existing), React-Bootstrap, Axios
**Storage**: H2 file-based (existing), nowe tabele: saved_repositories
**Testing**: JUnit 5, Mockito, Cucumber 7 (existing)
**Target Platform**: Linux server (self-hosted, Tailscale)
**Project Type**: Web application (existing)

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | Nowe porty/adaptery dla SavedRepository, zmiany w existing use cases |
| II. Provider Abstraction | PASS | Reuse existing MergeRequestProvider, bez zmian |
| III. BDD Testing | PASS | Nowe .feature files dla browse/cache/delete flow |
| IV. SDD Workflow | PASS | spec → plan → tasks |
| V. Simplicity (YAGNI) | PASS | Minimalne zmiany — reuse istniejacych komponentow |

## Project Structure

### Documentation (this feature)

```text
specs/002-mr-browse-analyze/
├── spec.md
├── plan.md
├── tasks.md
└── checklists/
    └── requirements.md
```

### Source Code — zmiany wzgledem 001-mvp-core

```text
backend/src/main/java/com/mranalizer/
├── domain/
│   ├── model/
│   │   └── SavedRepository.java              # NOWY — encja zapisanego repo
│   └── port/
│       ├── in/
│       │   ├── BrowseMrUseCase.java          # NOWY — pobieranie MR bez analizy
│       │   └── ManageReposUseCase.java        # NOWY — CRUD zapisanych repo
│       └── out/
│           └── SavedRepositoryPort.java       # NOWY — persystencja repo
├── application/
│   ├── AnalyzeMrService.java                  # ZMIANA — sprawdzanie cache przed analiza
│   ├── BrowseMrService.java                   # NOWY — implementacja BrowseMrUseCase
│   └── ManageReposService.java                # NOWY — implementacja ManageReposUseCase
└── adapter/
    ├── in/rest/
    │   ├── AnalysisRestController.java        # ZMIANA — DELETE endpoint, cache detection
    │   ├── RepoRestController.java            # NOWY — CRUD /api/repos
    │   └── BrowseRestController.java          # NOWY — POST /api/browse
    └── out/persistence/
        ├── entity/
        │   └── SavedRepositoryEntity.java     # NOWY — JPA entity
        ├── SpringDataSavedRepoRepository.java # NOWY — JPA repo
        └── JpaSavedRepositoryAdapter.java     # NOWY — impl portu

frontend/src/
├── api/
│   └── analysisApi.ts                         # ZMIANA — nowe endpointy (browse, repos, delete)
├── types/
│   └── index.ts                               # ZMIANA — nowe typy (SavedRepository, BrowseResult)
├── components/
│   ├── RepoSelector.tsx                       # NOWY — dropdown + input combo
│   ├── MrBrowseTable.tsx                      # NOWY — tabela MR bez scoringu
│   ├── AnalysisHistory.tsx                    # NOWY — lista zapisanych analiz z przyciskiem usun
│   └── AnalysisForm.tsx                       # ZMIANA — usuniety project slug (przeniesiony do RepoSelector)
├── pages/
│   ├── DashboardPage.tsx                      # ZMIANA — dwuetapowy flow, RepoSelector
│   └── MrDetailPage.tsx                       # ZMIANA — obsluga stanu bez analizy
└── styles/
    └── app.css                                # ZMIANA — nowe style
```

### Nowe REST endpointy

```
GET    /api/repos                    # lista zapisanych repozytoriow
POST   /api/repos                    # dodaj repo (body: {projectSlug, provider})
DELETE /api/repos/{id}               # usun repo z listy

POST   /api/browse                   # pobierz MR/PR bez analizy (body: criteria)
                                      # zwraca liste MergeRequest BEZ scoringu

DELETE /api/analysis/{reportId}      # usun zapisana analize

GET    /api/analysis?projectSlug=X   # sprawdz czy istnieje analiza dla repo
```

**Structure Decision**: Rozszerzenie istniejącej struktury — nowe pliki w existing packages, minimalne zmiany w existing kodzie.

## Complexity Tracking

No constitution violations. No complexity justification needed.
