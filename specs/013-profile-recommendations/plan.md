# Implementation Plan: 013 — Profile Recommendations

**Branch**: `013-profile-recommendations` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)

## Summary

Trzy poprawki z raportu profilowania: (1) N+1 fix — EntityGraph na findById, (2) early validation tokena GitHub w GitHubClient, (3) per-endpoint HTTP metrics w profile.sh.

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.2.5 + Bash
**Primary Dependencies**: Spring Data JPA (EntityGraph), istniejacy WebClient
**Storage**: H2 (bez zmian schematu)
**Testing**: BDD (Cucumber) + JUnit 5

## Constitution Check

| Gate | Status |
|------|--------|
| I. Hexagonal | PASS — zmiany w adapterach (persistence, github), zero zmian w domain |
| II. Provider Abstraction | PASS — walidacja tokena w GitHubClient (adapter), nie w porcie |
| III. BDD | PASS — nowe scenariusze dla US-1 (N+1) i US-2 (token validation) |
| IV. SDD | PASS — pelny flow |
| V. YAGNI | PASS — minimalne zmiany |

## Design Decisions

### D1: EntityGraph vs JOIN FETCH query
**Decyzja**: `@EntityGraph` na istniejacym `findById` w SpringDataAnalysisResultRepository.
**Uzasadnienie**: Prostsze niz custom JPQL z JOIN FETCH. Spring Data automatycznie obsluguje.

### D2: Token validation — gdzie?
**Decyzja**: W `GitHubClient` konstruktorze — zapisac flage `tokenConfigured`, sprawdzac w `fetchPullRequests`/`fetchPullRequest`/`fetchFiles`.
**Uzasadnienie**: Walidacja na poczatku metody — fail fast, bez HTTP call. GitHubClient jest najwyzszym miejscem gdzie token jest znany.

### D3: Per-endpoint metrics — jak?
**Decyzja**: W `profile.sh` iterowac po URI tagach z Actuator i query'owac kazdy osobno.
**Uzasadnienie**: Actuator nie eksportuje per-endpoint breakdown w jednym response — trzeba odpytac z tagiem `?tag=uri:/api/xxx`.

## Zmiany w plikach

| Plik | Zmiana |
|------|--------|
| `SpringDataAnalysisResultRepository.java` | +`@EntityGraph` na nowej metodzie findById z eager results |
| `JpaAnalysisResultRepository.java` | uzyc nowej metody z EntityGraph w findById |
| `GitHubClient.java` | +flaga tokenConfigured, +early validation w fetch metodach |
| `scripts/profile.sh` | zamiana raw JSON na tabele per-endpoint |
| `.feature` pliki BDD | nowe scenariusze |
| Unit testy | nowe testy GitHubClient token validation |
