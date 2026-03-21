# 010 — Remaining Review Fixes

## Cel

Dokoncenie poprawek z code review (review.md). Bezpieczenstwo, UX, a11y, architektura, czyszczenie.

## User Stories

### US-1: CORS i walidacja
- AC-1.1: CORS allowedMethods ograniczone do GET/POST/DELETE
- AC-1.2: spring-boot-starter-validation uzywany — @Valid na DTO, @NotBlank na polach
- AC-1.3: hibernate.ddl-auto=update zastapione przez validate + schema.sql

### US-2: Frontend UX/A11y
- AC-2.1: 404 catch-all route w App.tsx
- AC-2.2: Navbar.Brand uzywa React Router Link zamiast href
- AC-2.3: Route paths bez leading slash w nested routes
- AC-2.4: Clickable tr z tabIndex, role="link", onKeyDown(Enter)
- AC-2.5: Spinner z aria-label i visually-hidden text
- AC-2.6: Delete w historii — confirmation z informacja ile MR zostanie usunietych
- AC-2.7: Spojny jezyk — caly UI po polsku

### US-3: Architektura backend
- AC-3.1: ScoreBreakdownEntry.weight — zachowac faktyczna wage z RuleResult
- AC-3.2: AnalysisResultEntity — @Embedded grupowanie pol MR
