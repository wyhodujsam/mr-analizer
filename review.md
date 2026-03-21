# Code Review: mr_analizer

Data: 2026-03-21

---

## KRYTYCZNE BUGI

### 1. NPE w regulach scoringu (labels, diffStats, changedFiles)
**Pliki:** `ExcludeRule.java`, `BoostRule.java`, `PenalizeRule.java`

`mr.getLabels()`, `mr.getDiffStats()`, `mr.getChangedFiles()` moga zwracac `null`. Wywolanie `.stream()`, `.size()`, `.changedFilesCount()` na `null` rzuci NPE. MergeRequest builder nie defaultuje list do `List.of()`.

**Dotkniete metody:**
- `ExcludeRule.byLabels()` — iteracja po null labels
- `ExcludeRule.byMinChangedFiles()` / `byMaxChangedFiles()` — null diffStats
- `ExcludeRule.byFileExtensionsOnly()` — null changedFiles
- `BoostRule.byLabels()` — iteracja po null labels
- `BoostRule.byFilesRange()` — null diffStats
- `PenalizeRule.byLargeDiff()` — null diffStats
- `PenalizeRule.byTouchesConfig()` — null changedFiles

**Fix:** Defaultowac listy do `List.of()` i diffStats do zerowego rekordu w MergeRequest builder/constructor.

### 2. Stderr nie jest konsumowany w ClaudeCliAdapter
**Plik:** `ClaudeCliAdapter.java:76-87`

Jesli Claude CLI wypisze >64KB na stderr, bufor OS sie zapelni i proces zawiesi sie — `waitFor` bedzie czekal do timeout mimo ze proces nie jest wolny.

**Fix:** Uzyc `redirectErrorStream(true)` lub konsumowac stderr w osobnym watku.

### 3. Cache w BrowseMrService — race condition (TOCTOU)
**Plik:** `BrowseMrService.java:37-39`

`containsKey()` + `get()` to klasyczny TOCTOU race. Miedzy sprawdzeniem a pobraniem inny watek moze usunac wpis przez `invalidateCache()`, co spowoduje zwrocenie `null`.

**Fix:** Uzyc `computeIfAbsent()` zamiast check-then-act.

### 4. Cache key ignoruje kryteria poza projectSlug
**Plik:** `BrowseMrService.java:35`

Klucz cache to tylko `projectSlug`. Browse tego samego repo z roznymi datami/stanami/branchami zwroci stale wyniki z pierwszego zapytania.

**Fix:** Klucz cache powinien zawierac pelne FetchCriteria (lub ich hash).

### 5. Mutacja seenReports Set podczas renderowania (React StrictMode)
**Plik:** `AnalysisHistory.tsx:118-120`

Set jest mutowany wewnatrz `.map()` podczas renderowania — side-effect zakazany w concurrent mode i StrictMode. React double-invokuje render, wiec za drugim razem wszystko bedzie "already seen" i logika `isFirstInGroup` sie zepsuje.

**Fix:** Precompute Set/Map first-occurrence reportIds przed JSX return, nie wewnatrz `.map()`.

---

## BEZPIECZENSTWO

### 6. H2 console wlaczone z pustym haslem
**Plik:** `application.yml:78, 71`

`spring.h2.console.enabled: true` + puste haslo = kazdy kto dotrze do portu 8083 ma pelny dostep SQL do bazy danych.

**Fix:** Ograniczyc `h2.console.enabled: true` do profilu `dev`. W default profile `false`.

### 7. SSRF risk w GitHubClient
**Plik:** `GitHubClient.java:44`

`owner` i `repo` z user input sa interpolowane do URL przez `String.format`. Brak ochrony przed path traversal (np. `../../other-api`).

**Fix:** Uzyc `UriComponentsBuilder` z path segments zamiast String.format.

### 8. Brak obslugi bledow HTTP w GitHubClient
**Plik:** `GitHubClient.java`

WebClient `retrieve()` nie ma `.onStatus()` handlerow. 401/403/404 z GitHub API leca jako surowe `WebClientResponseException` → niezrozumialy 500 na froncie zamiast sensownego bledu.

**Fix:** Dodac `.onStatus()` mapujacy na `ProviderAuthException` (401/403), `ProviderException` (404), `ProviderRateLimitException` (429).

### 9. CORS zbyt permisywny
**Plik:** `CorsConfig.java:11-14`

`allowedMethods("*")` i `allowedHeaders("*")` jest nadmiernie permisywne. Wystarczy GET/POST/DELETE i standardowe headery.

---

## ARCHITEKTURA

### 10. BrowseRestController zalezy od konkretnej klasy, nie interfejsu
**Plik:** `BrowseRestController.java:20`

Controller zalezy od `BrowseMrService` zamiast `BrowseMrUseCase`. Metody `invalidateCache()` i `hasCachedResults()` nie sa na porcie — lamie architekture heksagonalna.

**Fix:** Dodac brakujace metody do `BrowseMrUseCase` albo stworzyc osobny port `BrowseCacheUseCase`.

### 11. Martwy bean: ProviderConfig.gitHubWebClient
**Plik:** `ProviderConfig.java:12-26`

`GitHubClient` tworzy wlasny `WebClient` w konstruktorze z `@Value`, ignorujac bean z `ProviderConfig`. Caly `ProviderConfig` to dead code.

**Fix:** Wstrzyknac bean do `GitHubClient` lub usunac `ProviderConfig`.

### 12. ScoringEngine — fragile exclude threshold
**Plik:** `ScoringEngine.java:21 vs 38-39`

`EXCLUDE_WEIGHT = -1000.0` ale sprawdzenie uzywa `<= -999.0`. Niespojne i kruche.

**Fix:** Sprawdzac `r.weight() == EXCLUDE_WEIGHT` lub `<= EXCLUDE_WEIGHT`.

### 13. DashboardPage — god component
**Plik:** `DashboardPage.tsx` — 388 linii, 15+ zmiennych stanu

Jeden komponent zarzadza: repami, selekcja, formularzem, wynikami browse, analiza, historia, loading, errory. Kazda zmiana stanu re-renderuje calosc ze wszystkimi child components.

**Fix:** Rozdzielic na mniejsze komponenty z wlasnym stanem. `useCallback` dla handlerow przekazywanych jako props, `useMemo` dla derived state.

---

## WYDAJNOSC

### 14. Paginacja GitHub pobiera wszystkie strony przed .limit()
**Pliki:** `GitHubClient.java:42-68`, `GitHubAdapter.java:50`

Limit = 5 ale 500 PR w repo? Wszystkie 500 zostanie pobrane z GitHub API (z pelna paginacja), a `.limit()` zadziala dopiero na streamie. Marnuje rate limit i czas.

**Fix:** Przekazac limit do `GitHubClient` i przerwac paginacje po osiagnieciu limitu.

### 15. Sekwencyjne wywolania LLM
**Plik:** `AnalyzeMrService.java:59-73`

Kazdy MR analizowany jest sekwencyjnie przez subprocess z timeout 60s. 10 MR = potencjalnie 10 minut. HTTP request prawdopodobnie timeout-uje na kliencie.

**Fix:** Zrownoleglenie (CompletableFuture/ExecutorService) lub async endpoint z polling po status.

### 16. getResult laduje caly raport zeby znalezc 1 wynik
**Plik:** `GetAnalysisResultsService.java:32-36`

Laduje caly raport z DB (ze wszystkimi wynikami), deserializuje JSONy, filtruje w pamieci.

**Fix:** Dodac query `findByReportIdAndId()` do repozytorium.

### 17. listReports filtruje w pamieci zamiast w DB
**Plik:** `AnalysisRestController.java:42-48`

Gdy `projectSlug` jest podany, wszystkie raporty ladowane z DB i filtrowane w pamieci.

**Fix:** Dodac query `findByProjectSlug()`.

---

## BRAKUJACA WALIDACJA

### 18. RepoRestController.addRepo — brak walidacji null projectSlug
**Plik:** `RepoRestController.java:33`

`body.get("projectSlug")` moze zwrocic null — persystuje repo z null slug.

**Fix:** Walidacja + 400 dla null/blank slug.

### 19. FetchCriteria — brak limitu gornego i walidacji zakresu dat
**Plik:** `FetchCriteria.java`

Klient moze wyslac `limit: 10000` → OOM/timeout. Brak walidacji ze `after < before`.

**Fix:** Max limit (np. 200) + walidacja after < before.

### 20. GitHubAdapter.fetchMergeRequest — NumberFormatException
**Plik:** `GitHubAdapter.java:63`

`Integer.parseInt(mrId)` bez try-catch → 500 zamiast 400 dla nieprawidlowego ID.

**Fix:** Try-catch z `InvalidRequestException`.

### 21. GitHubClient rzuca NoSuchElementException zamiast domain exception
**Plik:** `GitHubClient.java:79`

`NoSuchElementException` nie jest lapany przez `GlobalExceptionHandler` — leci jako 500.

**Fix:** Rzucac `ProviderException("PR not found: " + prNumber)`.

---

## FRONTEND — BUGI

### 22. Stale closure po deleteRepo
**Plik:** `DashboardPage.tsx:97`

Po `await loadRepos()`, `savedRepos` trzyma stara wartosc (React state async). `deleted` lookup dziala przez przypadek.

**Fix:** Zapisac `deleted` PRZED `loadRepos()`.

### 23. RepoSelector onBlur wywoluje submit
**Plik:** `RepoSelector.tsx:94`

Tab-owanie z inputa "new slug" triggeruje `handleNewSlugSubmit` — zaskakujace zachowanie UX.

**Fix:** Usunac `onBlur` handler lub dodac warunek (np. `relatedTarget` check).

### 24. Typy TypeScript nie pasuja do runtime
**Plik:** `types/index.ts`

`score: number`, `verdict: Verdict`, `labels: string[]` etc. deklarowane jako non-nullable, ale API zwraca `null`. Testy uzyja `as unknown as` zeby to obejsc. `ScoreBadge.tsx:19` — `score.toFixed(2)` crashuje jesli score jest null.

**Fix:** Zmienic typy na `score: number | null`, `verdict: Verdict | null` etc.

### 25. Ciche polykanie bledow
**Pliki:** `DashboardPage.tsx:59-61, 68-70, 83-89`

`loadRepos()`, `loadHistory()`, `addRepo()` — catch bloki nic nie robia. Jesli backend nie dziala, user widzi pusta strone bez informacji.

**Fix:** Ustawiac error state i wyswietlac komunikat.

### 26. Brak AbortController w useEffect
**Pliki:** `MrDetailPage.tsx`, `AnalysisDetailPage.tsx`

Brak anulowania requestow przy odmontowaniu komponentu → warning React 18 o setState na unmounted component.

**Fix:** Dodac `AbortController` w cleanup useEffect.

### 27. unsafe type cast handleRefresh
**Plik:** `DashboardPage.tsx:141-143`

`MouseEvent as unknown as FormEvent` — dziala przez przypadek bo `handleBrowse` uzywa tylko `preventDefault()`.

**Fix:** Wydzielic logike browse poza form handler.

---

## FRONTEND — UX

### 28. Brak 404 route
**Plik:** `App.tsx`

Nawigacja do nieistniejacego URL pokazuje pusta strone.

### 29. Navbar.Brand uzywa href zamiast React Router Link
**Plik:** `Layout.tsx:9`

`<Navbar.Brand href="/">` powoduje pelne przeladowanie strony.

### 30. AnalysisHistory delete usuwa caly raport, nie wiersz
Kazdy wiersz w tabeli ma "Usun" ktory kasuje caly raport (np. 50 wynikow MR). Brak wizualnej informacji o zasiegu operacji.

### 31. window.open moze byc zablokowany przez popup blocker
**Plik:** `MrBrowseTable.tsx:53`

### 32. Mieszany jezyk w UI
Wiekszosc po polsku, ale: "Score / Verdict", "Automatable", "Maybe", "Not Suitable", "by".

---

## DOSTEPNOSC (A11Y)

### 33. Klickable `<tr>` bez keyboard support
**Pliki:** `MrTable.tsx:42-44`, `AnalysisHistory.tsx:123-129`, `MrBrowseTable.tsx:79-84`

Rows sa klikalne przez `onClick` + `cursor: pointer`, ale brak `tabIndex`, `role="link"`, `onKeyDown`. Keyboard users nie moga nawigowac.

### 34. Spinner bez accessible labels
Brak `aria-label` i `<span className="visually-hidden">` na komponentach Spinner.

---

## DEAD CODE

### 35. `AnalysisForm.tsx` — caly plik nieuzywany
Nie importowany nigdzie. Dashboard buduje formularz inline.

### 36. `getSummary()`, `AnalysisSummary`, `ErrorResponse` w `types/index.ts`
Nigdzie nie uzywane.

### 37. Lombok w `pom.xml`
Zadeklarowany ale zero adnotacji Lombok w kodzie.

### 38. `spring-boot-starter-validation` w `pom.xml`
Zadeklarowany ale brak adnotacji `@Valid`, `@NotNull`, `@Size` w kodzie.

### 39. `ProviderConfig` — caly bean nieuzywany (patrz #11)

---

## ZDUPLIKOWANY KOD

### 40. `formatDate` — 3 kopie
Zdefiniowany oddzielnie w `MrBrowseTable.tsx`, `MrDetailPage.tsx`, `AnalysisHistory.tsx` z roznymi sygnaturami.

### 41. `verdictClass` — 2 kopie
Zdefiniowany w `MrTable.tsx` i `AnalysisHistory.tsx` z roznymi wartosciami zwracanymi.

### 42. `extractError` — 2 kopie
Pattern ekstrakcji bledow z Axios w `DashboardPage.tsx` i `MrDetailPage.tsx`.

### 43. `scoreBadgeColor` vs `ScoreBadge`
**Plik:** `AnalysisDetailPage.tsx:16-21`

Dwie rozne strategie kolorowania score w tej samej aplikacji (procentowa vs verdict-based).

---

## POMNIEJSZE

### 44. `isHasTests()` — bledna nazwa metody
**Plik:** `MergeRequest.java:68`

Powinno byc `hasTests()`.

### 45. `hibernate.ddl-auto=update` w domyslnym profilu
**Plik:** `application.yml:74`

Ryzykowne poza dev — moze tworzyc kolumny ale nie usunie/zmieni nazw. Powinno uzyc Flyway/Liquibase.

### 46. Podwojny srednik w imporcie
**Plik:** `ClaudeCliAdapter.java:5`

`import com.mranalizer.domain.model.*;;`

### 47. BoostRule.byDescriptionKeywords — mylaca nazwa
**Plik:** `BoostRule.java:57-59`

Deleguje do `byTitleKeywords` ktory szuka w title I description. Nazwa sugeruje ze szuka tylko w description.

### 48. MrDetailResponse.ScoreBreakdownEntry.weight zawsze 0.0
**Plik:** `MrDetailResponse.java:56`

Weight jest hardcoded na `0.0` — wagi regul nie sa zachowane w AnalysisResult. Breakdown jest mylacy dla uzytkownika.

### 49. AnalysisResultEntity — god class (25+ pol)
Mieszanie danych analizy z embedded MR metadata i JSON-serializowanymi obiektami. Warto rozwazyc `@Embedded` grupowanie.

### 50. Route paths z leading slash w nested routes
**Plik:** `App.tsx:13-14`

`"/mr/:reportId/:resultId"` wewnatrz nested `<Route path="/">` — React Router v6 wyswietla warning.

---

## PRIORYTETOWA KOLEJNOSC NAPRAWY

| Priorytet | # | Problem | Kategoria |
|-----------|---|---------|-----------|
| P0 | 1 | NPE w regulach (null labels/diffStats/changedFiles) | KRYTYCZNY BUG |
| P0 | 2 | Stderr w ClaudeCliAdapter — zawieszanie procesu | KRYTYCZNY BUG |
| P0 | 6 | H2 console + puste haslo | BEZPIECZENSTWO |
| P1 | 8 | Brak HTTP error handling w GitHubClient | BEZPIECZENSTWO |
| P1 | 5 | seenReports mutacja w renderze (StrictMode) | BUG |
| P1 | 3-4 | Cache TOCTOU + klucz ignorujacy kryteria | BUG |
| P1 | 24 | TypeScript typy vs nullable API fields | BUG |
| P1 | 25 | Ciche polykanie bledow na froncie | UX |
| P2 | 14 | Paginacja GitHub — pobieranie wszystkiego | WYDAJNOSC |
| P2 | 15 | Sekwencyjne wywolania LLM | WYDAJNOSC |
| P2 | 10-11 | Naruszenia architektury heksagonalnej | ARCHITEKTURA |
| P2 | 13 | DashboardPage god component | ARCHITEKTURA |
| P3 | 18-21 | Brakujaca walidacja | WALIDACJA |
| P3 | 35-39 | Dead code | CZYSTOSC |
| P3 | 40-43 | Zduplikowany kod | CZYSTOSC |
| P3 | 33-34 | Dostepnosc (a11y) | A11Y |
| P3 | 28-32 | Pomniejsze problemy UX | UX |
