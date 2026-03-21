# 007: Szczegóły analizy LLM — rozbudowana ocena + dedykowana strona

## Problem

Obecna analiza LLM zwraca tylko `scoreAdjustment` (double) i `comment` (string). Brakuje:
- **Dlaczego** LLM uważa, że PR da się zautomatyzować
- **Które fragmenty kodu** o tym świadczą (kategorie zmian z oceną)
- **Czynniki** wpływające na ocenę (technologia, architektura, wzorce)
- **Co wymaga nadzoru człowieka** i dlaczego
- **Podsumowania** z rozbiciem na aspekty (wykonanie, decyzje, testy, review)

Użytkownik nie ma gdzie zobaczyć tych szczegółów — brakuje dedykowanej strony.

## Wymagania

### 1. Rozbudowana odpowiedź LLM

LLM ma zwracać strukturyzowany JSON z polami:

```json
{
  "scoreAdjustment": 0.4,
  "comment": "Krótkie podsumowanie (1-2 zdania)",
  "overallAutomatability": 90,
  "categories": [
    {
      "name": "Split god class (CQRS)",
      "score": 95,
      "reasoning": "Czysto mechaniczna ekstrakcja metod query do nowej klasy..."
    }
  ],
  "humanOversightRequired": [
    {
      "area": "Decyzja architektoniczna 'co splitować'",
      "reasoning": "LLM nie podejmie sam decyzji, że to god class..."
    }
  ],
  "whyLlmFriendly": [
    "Brak kreatywności domenowej — refaktoring architektoniczny",
    "Jasne wzorce (CQRS, DRY) — LLM trenował na milionach repo",
    "Istniejące testy jako siatka bezpieczeństwa"
  ],
  "summaryTable": [
    { "aspect": "Wykonanie zmian kodu", "score": 95, "note": "automatyzowalne" },
    { "aspect": "Podjęcie decyzji 'co zmienić'", "score": 50, "note": "wymaga instrukcji od człowieka" },
    { "aspect": "Napisanie testów", "score": 85, "note": "automatyzowalne" },
    { "aspect": "Nadzór/review", "score": null, "note": "Konieczny, ale lekki" }
  ]
}
```

### 2. Nowe modele domenowe

- `LlmAssessment` — rozbudować o nowe pola (categories, humanOversight, whyLlmFriendly, summaryTable, overallAutomatability)
- `AnalysisCategory` — record: name, score, reasoning
- `HumanOversightItem` — record: area, reasoning
- `SummaryAspect` — record: aspect, score (nullable Integer), note
- `AnalysisResult` — dodać pola z rozbudowanej analizy LLM (lub referencję do pełnego LlmAssessment)

### 3. Persystencja

- Nowe pola w `AnalysisResultEntity` — serializowane jako JSON (jak obecne `reasons`)
- Migracja: istniejące dane bez nowych pól = null (graceful fallback)

### 4. REST API

- Istniejący endpoint `GET /api/analysis/{reportId}/mrs/{resultId}` — rozszerzyć `MrDetailResponse` o nowe pola
- Nowy endpoint lub rozszerzenie: zwraca pełne szczegóły analizy LLM

### 5. Nowa strona frontend: Analysis Detail Page

- **Route**: `/analysis/:reportId/:resultId` (osobna od `/mr/:reportId/:resultId`)
- **Nawigacja**: przycisk "Szczegóły analizy" na MrDetailPage oraz w tabeli wyników na DashboardPage
- **Zawartość strony**:

  a) **Nagłówek**: tytuł PR, autor, zakres (pliki, linie, commity)

  b) **Ocena ogólna**: "LLM (provider) poradzi sobie z tym zadaniem na ~X%"

  c) **Tabela kategorii zmian**: kolumny: Kategoria | Ocena | Uzasadnienie

  d) **Sekcja "Co wymaga nadzoru człowieka"**: lista z uzasadnieniami

  e) **Sekcja "Dlaczego ten PR jest LLM-friendly"**: bullet points

  f) **Tabela podsumowania**: aspekt | ocena (jak w przykładzie usera)

  g) **Komentarz LLM**: oryginalny komentarz tekstowy

### 6. Prompt LLM

- Zaktualizować `PromptBuilder` / domyślny template aby wymagał strukturyzowanej odpowiedzi JSON z nowymi polami
- Prompt musi jasno instruować LLM co zwrócić i w jakim formacie

## Scenariusze użycia

### UC1: Analiza PR z rozbudowanymi szczegółami LLM

**Aktor**: Użytkownik (deweloper/tech lead)
**Warunek wstępny**: Użytkownik ma zapisane repo i przeglądnięte MR-y na DashboardPage.

1. Użytkownik zaznacza PR-y do analizy, włącza checkbox "Użyj LLM" i klika "Analizuj".
2. System wysyła każdy PR do LLM z rozbudowanym promptem wymagającym strukturyzowanego JSON.
3. LLM zwraca: ogólną ocenę automatyzowalności (%), kategorie zmian z oceną i uzasadnieniem, obszary wymagające nadzoru człowieka, powody dlaczego PR jest LLM-friendly, tabelę podsumowania per aspekt.
4. System zapisuje rozbudowaną analizę w bazie.
5. Na DashboardPage w tabeli wyników pojawia się kolumna/przycisk "Szczegóły analizy" przy każdym PR, który ma dane z LLM.

### UC2: Przeglądanie szczegółów analizy LLM

**Aktor**: Użytkownik
**Warunek wstępny**: Istnieje analiza PR z danymi LLM.

1. Użytkownik klika "Szczegóły analizy" przy wybranym PR (z tabeli na DashboardPage lub z MrDetailPage).
2. System otwiera dedykowaną stronę `/analysis/:reportId/:resultId`.
3. Strona wyświetla:
   - Nagłówek: tytuł PR, autor, zakres zmian (pliki, +/- linie)
   - Ocena ogólna: "LLM (Opus) poradzi sobie z tym zadaniem na ~90%"
   - Tabela kategorii zmian z kolumnami: Kategoria | Ocena (%) | Uzasadnienie
   - Sekcja "Co wymaga nadzoru człowieka" — lista punktów z uzasadnieniem
   - Sekcja "Dlaczego ten PR jest LLM-friendly" — bullet points
   - Tabela podsumowania: Aspekt | Ocena
   - Komentarz LLM (tekst)
4. Użytkownik może wrócić do DashboardPage lub przejść do MrDetailPage (nawigacja).

### UC3: Przeglądanie starej analizy bez danych LLM (backward compatibility)

**Aktor**: Użytkownik
**Warunek wstępny**: Istnieje analiza PR sprzed wdrożenia feature 007 (brak rozbudowanych danych LLM).

1. Użytkownik otwiera stronę szczegółów analizy starego PR.
2. System wyświetla dostępne dane (tytuł, score, verdict, komentarz LLM jeśli był).
3. Sekcje kategorii, nadzoru, LLM-friendly i podsumowania nie są wyświetlane (graceful fallback — brak danych = brak sekcji, bez błędów).

### UC4: Analiza bez LLM — brak przycisku szczegółów

**Aktor**: Użytkownik
**Warunek wstępny**: Użytkownik uruchomił analizę BEZ włączonego LLM.

1. Użytkownik analizuje PR-y bez checkboxa "Użyj LLM".
2. W tabeli wyników przycisk "Szczegóły analizy" nie pojawia się (lub jest disabled) — nie ma danych do pokazania.
3. Użytkownik nadal może przejść do MrDetailPage (istniejąca funkcjonalność score breakdown).

### UC5: LLM zwraca niepełny/uszkodzony JSON

**Aktor**: System (automatyczny)
**Warunek wstępny**: LLM nie zwrócił wszystkich wymaganych pól lub JSON jest nieprawidłowy.

1. System parsuje odpowiedź LLM.
2. Brakujące pola otrzymują wartości domyślne (puste listy, null).
3. `scoreAdjustment` i `comment` (pola krytyczne) — jeśli brakuje, analiza oznaczana z domyślnym scoreAdjustment=0 i komentarzem "Analiza LLM nie zwróciła pełnych danych".
4. Pozostałe dane (kategorie, nadzór, itd.) — opcjonalne, brak = pusta lista.
5. Strona szczegółów wyświetla tylko te sekcje, dla których są dane.

## Poza zakresem

- Zmiana adaptera LLM (zostaje claude-cli)
- Nowe reguły scoringowe
- GitLab adapter
- Eksport do PDF/CSV

## Kryteria akceptacji

### Analiza LLM (backend)

1. **Rozbudowany prompt**: `PromptBuilder` generuje prompt instruujący LLM aby zwrócił JSON z polami: `scoreAdjustment`, `comment`, `overallAutomatability`, `categories`, `humanOversightRequired`, `whyLlmFriendly`, `summaryTable`.
2. **Parsing odpowiedzi**: `ClaudeCliAdapter` parsuje rozbudowany JSON i tworzy `LlmAssessment` z nowymi polami. Brakujące pola opcjonalne → puste listy/null (nie exception).
3. **Propagacja do AnalysisResult**: `ScoringEngine` przepisuje nowe pola z `LlmAssessment` do `AnalysisResult`.
4. **Persystencja**: nowe pola serializowane jako JSON w `AnalysisResultEntity`, odtwarzalne po restarcie aplikacji.
5. **Backward compatibility danych**: istniejące rekordy bez nowych pól ładują się poprawnie (null → puste kolekcje w domenie).

### REST API

6. **Rozszerzony MrDetailResponse**: endpoint `GET /api/analysis/{reportId}/mrs/{resultId}` zwraca nowe pola: `overallAutomatability`, `categories`, `humanOversightRequired`, `whyLlmFriendly`, `summaryTable`.
7. **Flaga w liście wyników**: `AnalysisResponse.ResultItem` zawiera pole `hasDetailedAnalysis` (boolean) — true gdy są dane z rozbudowanej analizy LLM.

### Frontend — strona szczegółów

8. **Nowa route**: `/analysis/:reportId/:resultId` renderuje `AnalysisDetailPage`.
9. **Nagłówek**: tytuł PR, autor, zakres (pliki, +/- linie).
10. **Ocena ogólna**: wyświetla "LLM ({provider}) poradzi sobie z tym zadaniem na ~{overallAutomatability}%".
11. **Tabela kategorii**: renderuje `categories` w tabeli: Kategoria zmian | Ocena (%) | Uzasadnienie.
12. **Nadzór człowieka**: sekcja z listą `humanOversightRequired` — każdy element: nagłówek (area) + opis (reasoning).
13. **Dlaczego LLM-friendly**: bullet points z `whyLlmFriendly`.
14. **Tabela podsumowania**: renderuje `summaryTable` — Aspekt | Ocena.
15. **Komentarz LLM**: wyświetla `llmComment` jako tekst.

### Frontend — nawigacja

16. **Przycisk na DashboardPage**: w tabeli wyników (MrTable) — przycisk/link "Szczegóły analizy" przy PR-ach z `hasDetailedAnalysis=true`. Disabled/ukryty gdy brak danych LLM.
17. **Przycisk na MrDetailPage**: link do strony szczegółów analizy (widoczny tylko gdy są dane LLM).
18. **Nawigacja zwrotna**: ze strony szczegółów można wrócić do DashboardPage.

### Graceful fallback

19. **Stare analizy**: strona szczegółów dla analizy bez danych LLM wyświetla tylko dostępne informacje (tytuł, score, verdict) — sekcje kategorii/nadzoru/podsumowania nie renderują się, brak błędów.
20. **Analiza bez LLM**: PR-y analizowane bez checkboxa LLM nie mają przycisku "Szczegóły analizy".

### Testy

21. **BDD scenario**: happy path — analiza z LLM → dane zapisane → endpoint zwraca rozbudowaną strukturę.
22. **BDD scenario**: fallback — analiza bez LLM → brak pól rozbudowanych → brak błędów.
23. **Unit test**: `LlmAssessment` — parsowanie pełnego i częściowego JSON.
24. **Unit test**: `ScoringEngine` — propagacja nowych pól do `AnalysisResult`.
25. **Unit test**: `PromptBuilder` — nowy template zawiera instrukcje dla strukturyzowanego JSON.
