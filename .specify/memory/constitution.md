# MR Analizer Constitution

## Core Principles

### I. Hexagonal Architecture (MUST)

Application MUST follow hexagonal (ports & adapters) architecture with strict dependency rules:
- **Domain** (core): Pure business logic, zero framework dependencies. Domain MUST NOT import Spring, JPA, or any adapter code.
- **Ports**: Interfaces defined in domain that adapters implement. Inbound ports (use cases) and outbound ports (SPI).
- **Adapters**: Implementations of ports — web controllers, persistence, external API clients, LLM connectors.
- **Dependency direction**: Adapters depend on domain, NEVER the reverse.
- Package structure: `domain/`, `application/`, `adapter/in/`, `adapter/out/`, `adapter/config/`

### II. Provider Abstraction (MUST)

All external data sources (GitHub, GitLab) and LLM connectors MUST be accessed through port interfaces.
- `MergeRequestProvider` port for VCS providers — swappable without touching domain logic.
- `LlmAnalyzer` port for LLM integrations — swappable without touching domain logic.
- Adding a new provider or LLM MUST NOT require changes to domain or application layers.
- Configuration selects active adapter at runtime via `application.yml`.

### III. BDD Testing (MUST)

BDD (Behavior-Driven Development) is a test-first, agile testing practice. Scenarios describe behavior from the user's perspective, not internal implementation. BDD is a synthesis and refinement of TDD and ATDD practices.

- Acceptance tests MUST be written as Cucumber scenarios in Gherkin format (Given/When/Then).
- `.feature` files in `backend/src/test/resources/features/`
- Step definitions in `backend/src/test/java/com/mranalizer/bdd/steps/`
- Scenarios MUST be business-facing — describing behavior of a story/feature/capability from user perspective.
- Scenarios MUST map to user stories and acceptance criteria from spec.md.
- Scenarios MUST NOT contain implementation details (no class names, SQL, HTTP codes in scenario text).
- New features MUST have corresponding .feature files BEFORE implementation (test-first).
- `.feature` files serve as living documentation of system behavior.
- Unit tests (JUnit 5 + Mockito) for domain logic remain mandatory alongside BDD tests.

### III-b. Test Pyramid (MUST)

Projekt utrzymuje piramidę testów opisaną w `TEST_PYRAMID.md`. Każda nowa funkcjonalność MUSI zachować proporcje piramidy:

```
         E2E (Playwright)         — najmniej, najwolniejsze, najdroższe
        Integration (Spring+MockWebServer) — umiarkowanie
       BDD (Cucumber/Gherkin)     — scenariusze biznesowe
      Unit (JUnit, Vitest)        — najwięcej, najszybsze
```

**Wymagania przy specyfikacji nowej funkcjonalności (spec.md / plan.md / tasks.md):**

1. **Unit tests** — KAŻDY nowy domain service, rule, calculator, detector MUSI mieć unit testy. Pokrycie edge cases (empty input, null, division by zero).
2. **BDD scenarios** — KAŻDA user story ze spec.md MUSI mieć odpowiadające scenariusze `.feature` PRZED implementacją.
3. **Integration tests** — KAŻDY nowy REST endpoint MUSI mieć integration test z MockWebServer (full round-trip: HTTP → controller → service → mock provider → response JSON).
4. **E2E tests** — KAŻDA nowa strona/widok MUSI mieć co najmniej 1 E2E test Playwright (nawigacja + podstawowy flow).
5. **Frontend unit tests** — KAŻDY nowy React komponent MUSI mieć Vitest test (renderowanie z mock data, interakcje).
6. **Persistence round-trip** — KAŻDA nowa encja JPA MUSI mieć test serializacji/deserializacji (szczególnie JSON fields, enum mapping).
7. **Concurrency** — jeśli feature używa parallel execution, cache, lub shared state → MUSI mieć test na thread safety.

**Checklist do tasks.md:**
- [ ] Unit testy domain logic
- [ ] BDD .feature file (test-first)
- [ ] Integration test REST endpoints
- [ ] Frontend component test (Vitest)
- [ ] E2E test (Playwright) — jeśli nowa strona/flow
- [ ] JPA round-trip test — jeśli nowa encja
- [ ] Concurrency test — jeśli shared state

### IV. SDD Workflow (MUST)

All features MUST follow the Spec Kit workflow:
- Specify → Plan → Tasks → Analyze → Implement
- Feature branches: `###-feature-name`
- Artifacts in `specs/###-feature-name/`
- No implementation without spec.md and plan.md
- Constitution compliance checked at every planning gate.

### V. Simplicity (YAGNI)

- No premature abstractions — three similar lines better than early refactoring.
- Only refactor when duplication becomes maintenance burden.
- No features beyond current phase scope.
- Prefer Spring Boot conventions and auto-configuration.
- Start simple, evolve incrementally.

## Technology Stack

- **Backend**: Java 17 + Spring Boot 3.x (Web, Validation, Data JPA, WebFlux client)
- **Frontend**: React 18 + TypeScript + Vite + Bootstrap 5 (React-Bootstrap)
- **Build**: Maven (backend), npm (frontend)
- **Database**: H2 (dev/test), PostgreSQL (prod option)
- **Testing**: JUnit 5, Mockito, Cucumber 7, Spring Test
- **Ports**: 8083 (backend), 3000 (frontend dev)
- **Packaging**: JAR (backend, embedded Tomcat) — produkcja: frontend build serwowany przez backend

## Governance

- Constitution supersedes all other practices.
- Amendments require documentation, version bump, and migration plan.
- All spec reviews must verify compliance with hexagonal architecture and BDD principles.
- Complexity beyond constitution principles must be justified in plan.md Complexity Tracking table.

**Version**: 1.1.0 | **Ratified**: 2026-03-20 | **Last Amended**: 2026-03-25
