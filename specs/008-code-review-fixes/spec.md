# 008 — Code Review Fixes

## Cel

Sprint naprawczy wynikajacy z code review (2026-03-21). Naprawa krytycznych bugow, luk bezpieczenstwa, brakujacej walidacji, problemow wydajnosciowych i naruszen architektury. Poprawa jakosci frontendu (typy, error handling, dead code).

## Zakres

Na podstawie review.md — 50 problemow pogrupowanych w 3 fazy (P0 → P1 → P2/P3).

---

## User Stories

### US-1: Odporna analiza MR (NPE safety)
**Jako** uzytkownik analizujacy MR
**Chce** zeby analiza nie crashowala na MR z brakiem labels/diffStats/changedFiles
**Abym** mogl analizowac dowolne PR bez 500 Internal Server Error

**Acceptance Criteria:**
- AC-1.1: MR bez labels nie powoduje NPE w ExcludeRule.byLabels i BoostRule.byLabels
- AC-1.2: MR bez diffStats nie powoduje NPE w ExcludeRule.byMinChangedFiles, byMaxChangedFiles, BoostRule.byFilesRange, PenalizeRule.byLargeDiff
- AC-1.3: MR bez changedFiles nie powoduje NPE w PenalizeRule.byTouchesConfig i ExcludeRule.byFileExtensionsOnly
- AC-1.4: MR z null labels/diffStats/changedFiles otrzymuje domyslne wartosci (puste listy, zerowe statystyki)

### US-2: Stabilny adapter LLM
**Jako** uzytkownik uruchamiajacy analize z LLM
**Chce** zeby adapter Claude CLI nie zawieszal sie
**Abym** dostal wynik lub sensowny fallback w rozsdnym czasie

**Acceptance Criteria:**
- AC-2.1: Stderr Claude CLI jest konsumowane — proces nie zawiesza sie na duzym wyjsciu stderr
- AC-2.2: Timeout 60s dziala poprawnie nawet przy duzym stderr

### US-3: Bezpieczna konfiguracja
**Jako** operator
**Chce** zeby H2 console nie bylo dostepne z pustym haslem w domyslnym profilu
**Abym** nie narazal bazy danych na nieautoryzowany dostep

**Acceptance Criteria:**
- AC-3.1: H2 console wylaczone w domyslnym profilu (tylko profil dev)
- AC-3.2: GitHubClient uzywa UriComponentsBuilder zamiast String.format dla URL (SSRF)
- AC-3.3: GitHubClient obsluguje HTTP 401/403/404/429 z sensownymi domain exceptions

### US-4: Poprawny cache browse
**Jako** uzytkownik przegladajacy MR
**Chce** zeby cache uwzglednialo pelne kryteria wyszukiwania
**Abym** nie dostawal starych wynikow po zmianie filtra dat/stanu/brancha

**Acceptance Criteria:**
- AC-4.1: Cache key uwzglednia projectSlug + state + branch + after + before + limit
- AC-4.2: Brak race condition — uzycie computeIfAbsent zamiast containsKey+get
- AC-4.3: BrowseRestController zalezy od portu (interfejsu), nie od konkretnej klasy

### US-5: Walidacja inputu
**Jako** uzytkownik API
**Chce** zeby bledne dane wejsciowe zwracaly 400 z sensownym komunikatem
**Abym** mogl naprawic swoj request

**Acceptance Criteria:**
- AC-5.1: Null/blank projectSlug w addRepo zwraca 400
- AC-5.2: Nieprawidlowy mrId (nie-numer) zwraca 400, nie 500
- AC-5.3: FetchCriteria: limit max 200, after < before validation
- AC-5.4: GitHubClient: PR not found rzuca ProviderException, nie NoSuchElementException

### US-6: Poprawne typy i error handling na froncie
**Jako** uzytkownik frontendu
**Chce** widziec komunikaty bledow gdy backend nie dziala
**Abym** wiedzial co sie dzieje zamiast patrzec na pusta strone

**Acceptance Criteria:**
- AC-6.1: TypeScript typy odzwierciedlaja nullable pola z API (score, verdict, labels, etc.)
- AC-6.2: loadRepos/loadHistory/addRepo pokazuja blad zamiast go polykac
- AC-6.3: ScoreBadge nie crashuje na null score
- AC-6.4: seenReports w AnalysisHistory nie mutuje podczas renderowania
- AC-6.5: AbortController w useEffect (MrDetailPage, AnalysisDetailPage)

### US-7: Usuniecie dead code
**Jako** deweloper
**Chce** zeby kod nie zawieral nieuzywanych plikow/klas/metod
**Abym** latwiej orientowal sie w codebase

**Acceptance Criteria:**
- AC-7.1: AnalysisForm.tsx usuniety (dead code)
- AC-7.2: getSummary, AnalysisSummary, ErrorResponse w types/index.ts usuniete
- AC-7.3: ProviderConfig usuniety (martwy bean)
- AC-7.4: Lombok i spring-boot-starter-validation: albo usunac albo zaczac uzywac
- AC-7.5: Podwojny srednik w ClaudeCliAdapter naprawiony
- AC-7.6: Zduplikowany kod (formatDate, verdictClass, extractError) wydzielony do wspolnych utilsow
