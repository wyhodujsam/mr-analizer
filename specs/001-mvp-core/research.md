# Research: MVP Core

## GitHub API — Pull Requests

**Decision**: Uzyc GitHub REST API v3 (nie GraphQL) przez Spring WebClient.
**Rationale**: REST API jest prostsze, dobrze udokumentowane, wystarczajace dla MVP. WebClient (reactive) daje async i jest czescia Spring ekosystemu.
**Alternatives**: GraphQL (zbyt skomplikowany na MVP), RestTemplate (deprecated), Feign (dodatkowa zaleznosc).

### Kluczowe endpointy

- `GET /repos/{owner}/{repo}/pulls?state=closed&sort=updated&direction=desc&per_page=100` — lista PR
- `GET /repos/{owner}/{repo}/pulls/{number}/files` — zmienione pliki
- Paginacja: `Link` header z `rel="next"`
- Auth: `Authorization: Bearer {token}`
- Rate limit: 5000 req/h (authenticated), header `X-RateLimit-Remaining`

### Mapowanie na domain model

| GitHub API field | MergeRequest field |
|---|---|
| number | externalId |
| title | title |
| body | description |
| user.login | author |
| head.ref | sourceBranch |
| base.ref | targetBranch |
| state + merged_at | state (merged/closed/open) |
| created_at | createdAt |
| merged_at | mergedAt |
| labels[].name | labels |
| html_url | url |

## Cucumber 7 + Spring Boot 3

**Decision**: cucumber-java + cucumber-junit-platform-engine + cucumber-spring
**Rationale**: Oficjalna integracja Cucumber ze Spring Boot i JUnit Platform. Cucumber-spring zapewnia Spring context w step definitions.
**Alternatives**: cucumber-junit (stary runner, JUnit 4) — deprecated.

### Maven dependencies

```xml
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.18.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <version>7.18.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.18.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.platform</groupId>
    <artifactId>junit-platform-suite</artifactId>
    <scope>test</scope>
</dependency>
```

### Konfiguracja

- `CucumberTestRunner.java`: `@Suite @SelectClasspathResource("features")`
- `CucumberSpringConfig.java`: `@CucumberContextConfiguration @SpringBootTest`
- `.feature` files w `src/test/resources/features/`

## Claude CLI Adapter

**Decision**: Wywolanie `claude -p "prompt" --output-format json` jako Process, parsowanie stdout.
**Rationale**: Dziala na istniejacej subskrypcji, zero kosztow API. Latwe do podmienienia na API w przyszlosci (ten sam port LlmAnalyzer).
**Alternatives**: Claude API (wymaga API key, koszty), OpenAI (inny provider).

### Implementacja

- `ProcessBuilder` z timeout 60s
- Prompt zawiera: tytul, opis, liste zmienionych plikow, diff stats
- Output: score adjustment (-0.5 do +0.5) + uzasadnienie
- Error handling: timeout → fallback NoOp, exit code != 0 → log + fallback

## React Frontend

**Decision**: Vite + React 18 + TypeScript + React-Bootstrap + Axios + React Router v6
**Rationale**: Vite jest najszybszym bundlerem, React-Bootstrap daje gotowe komponenty Bootstrap 5, Axios jest standardem do HTTP.
**Alternatives**: CRA (wolny, deprecated), Next.js (SSR niepotrzebny), Angular (zbyt ciezki).

### Proxy dev

Vite proxy w `vite.config.ts`: `/api` → `http://localhost:8083`

## H2 + JPA

**Decision**: H2 in-memory (dev), Spring Data JPA z prostymi entities.
**Rationale**: Zero konfiguracji, automatyczne tworzenie schematu, wystarczajace dla MVP.
**Alternatives**: PostgreSQL (za ciezki na MVP dev), SQLite (slaba integracja ze Spring).
