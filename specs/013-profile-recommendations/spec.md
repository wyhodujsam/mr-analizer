# 013 — Profile Recommendations (poprawki z raportu profilowania)

## Cel

Implementacja 3 rekomendacji z raportu profilowania wydajnosci (`reports/profile-20260322-105903.md`): naprawa N+1 w kolekcjach JPA, early validation tokena GitHub, rozszerzenie raportu profilowania o metryki per-endpoint.

## User Stories

### US-1: Naprawa N+1 w ladowaniu kolekcji AnalysisReport → AnalysisResult

**Jako** uzytkownik API
**Chce** zeby endpoint GET /api/analysis/{reportId} ladownal raport z wynikami w jednym zapytaniu SQL
**Abym** dostal szybka odpowiedz nawet przy duzej liczbie wynikow

**AC:**
- AC-1.1: GET /api/analysis/{reportId} wykonuje maksymalnie 2 zapytania SQL (raport + wyniki) zamiast N+1
- AC-1.2: GET /api/analysis (lista raportow) nie laduje wynikow (lazy) — tylko metadane raportu
- AC-1.3: Istniejace testy nadal przechodza

### US-2: Early validation tokena GitHub

**Jako** uzytkownik przegladajacy MR
**Chce** zeby system natychmiast informowal o braku tokena GitHub
**Abym** nie czekal 2 sekundy na timeout HTTP zanim dostane komunikat o bledzie

**AC:**
- AC-2.1: POST /api/browse z pustym/brakujacym GITHUB_TOKEN zwraca blad w <100ms (zamiast ~2s timeout)
- AC-2.2: Komunikat bledu jasno informuje o braku tokena ("GitHub token is not configured")
- AC-2.3: HTTP status 401 (Unauthorized) zamiast 502 (Bad Gateway)

### US-3: Metryki HTTP per-endpoint w raporcie profilowania

**Jako** developer
**Chce** widziec czasy odpowiedzi per endpoint w raporcie profilowania
**Abym** mogl zidentyfikowac ktory endpoint jest najwolniejszy

**AC:**
- AC-3.1: Raport zawiera tabele z metrykami per endpoint: URI, method, count, avg time, max time
- AC-3.2: Tabela sortowana od najwolniejszego endpointu
- AC-3.3: Zastepuje surowy JSON blob czytelna tabela
