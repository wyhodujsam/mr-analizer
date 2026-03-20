# mr_analizer Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-20

## Active Technologies
- Java 17 (backend), TypeScript 5.x (frontend) + Spring Boot 3.x (Web, Data JPA, Validation, WebFlux client), React 18, Vite, React-Bootstrap, Axios, React Router, Lombok (001-mvp-core)
- H2 (dev/test), Spring Data JPA (001-mvp-core)

- Java 17 + Spring Boot 3.x (Web, Data JPA, WebFlux client) — backend
- React 18 + TypeScript + Vite — frontend
- Cucumber 7 + JUnit 5 + Mockito (testing)
- H2 (dev database)
- Bootstrap 5 / React-Bootstrap (UI)

## Project Structure

```text
backend/                              # Spring Boot REST API (hexagonal: domain/, application/, adapter/)
frontend/                             # React + TypeScript SPA
specs/                                # SDD feature specs
.specify/                             # Spec Kit infrastructure
```

## Architecture

Hexagonal (ports & adapters) backend + React SPA frontend. See `.specify/memory/constitution.md` for rules.

## Testing Approach — BDD

BDD (Behavior-Driven Development) — test-first, scenarios describe system behavior from user perspective.
- Write `.feature` files (Gherkin) BEFORE implementation
- Scenarios map to user stories from spec.md
- Business-facing language, no implementation details in scenarios
- Unit tests (JUnit 5 + Mockito) complement BDD for domain logic

## Commands

```bash
# Backend
cd backend && mvn clean install       # build
cd backend && mvn test                # all tests (unit + Cucumber)
cd backend && mvn spring-boot:run     # run on port 8083

# Frontend
cd frontend && npm install            # install deps
cd frontend && npm run dev            # dev server on port 3000
cd frontend && npm run build          # production build
```

## Code Style

- Java 17: Follow standard conventions. Lombok for boilerplate reduction.
- TypeScript: Strict mode. Functional components with hooks.

## Recent Changes
- 001-mvp-core: Added Java 17 (backend), TypeScript 5.x (frontend) + Spring Boot 3.x (Web, Data JPA, Validation, WebFlux client), React 18, Vite, React-Bootstrap, Axios, React Router, Lombok

- Initial project setup

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
