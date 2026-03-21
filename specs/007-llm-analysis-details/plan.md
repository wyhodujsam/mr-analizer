# 007: Plan implementacji

## Warstwy zmian (od wewnątrz hexagonu na zewnątrz)

### Warstwa 1: Domain models (pure Java, 0 deps)

1. **Nowe rekordy** w `domain/model/`:
   - `AnalysisCategory(String name, int score, String reasoning)`
   - `HumanOversightItem(String area, String reasoning)`
   - `SummaryAspect(String aspect, Integer score, String note)`

2. **Rozbudowa `LlmAssessment`** — dodanie pól:
   - `int overallAutomatability`
   - `List<AnalysisCategory> categories`
   - `List<HumanOversightItem> humanOversightRequired`
   - `List<String> whyLlmFriendly`
   - `List<SummaryAspect> summaryTable`
   - Zmiana z record na record z większą liczbą pól (backward-compatible: builder z defaults)

3. **Rozbudowa `AnalysisResult`** — dodanie pól z LlmAssessment:
   - `int overallAutomatability`
   - `List<AnalysisCategory> categories`
   - `List<HumanOversightItem> humanOversightRequired`
   - `List<String> whyLlmFriendly`
   - `List<SummaryAspect> summaryTable`

### Warstwa 2: Domain scoring

4. **`PromptBuilder`** — nowy DEFAULT_TEMPLATE z instrukcjami dla LLM aby zwracał rozbudowany JSON

5. **`ScoringEngine`** — przekazanie nowych pól z LlmAssessment do AnalysisResult

### Warstwa 3: Adapter out — persistence

6. **`AnalysisResultEntity`** — nowe kolumny JSON:
   - `overallAutomatability` (Integer, nullable)
   - `categories` (String/TEXT, JSON)
   - `humanOversightRequired` (String/TEXT, JSON)
   - `whyLlmFriendly` (String/TEXT, JSON)
   - `summaryTable` (String/TEXT, JSON)

7. **`JpaAnalysisResultRepository`** — mapping nowych pól (JSON serialize/deserialize)

### Warstwa 4: Adapter out — LLM

8. **`ClaudeCliAdapter`** — parsowanie rozbudowanego JSON response (nowe pola)

### Warstwa 5: Adapter in — REST

9. **`MrDetailResponse`** — nowe pola w DTO
10. **`AnalysisRestController`** — mapping nowych pól w response (getResult)
11. **`AnalysisResponse.ResultItem`** — dodać `overallAutomatability` i flagę `hasDetailedAnalysis`

### Warstwa 6: Frontend

12. **Nowe typy TypeScript** — `AnalysisCategory`, `HumanOversightItem`, `SummaryAspect` + rozszerzenie `MrDetailResponse`
13. **`AnalysisDetailPage`** — nowa strona React z sekcjami: nagłówek, ocena, kategorie, nadzór, LLM-friendly, podsumowanie
14. **Route** w `App.tsx` — `/analysis/:reportId/:resultId`
15. **Nawigacja** — przycisk "Szczegóły analizy" w `MrTable` (DashboardPage) i `MrDetailPage`

### Warstwa 7: Testy

16. **BDD** — feature file: analiza z LLM → strona szczegółów wyświetla kategorie
17. **Unit** — LlmAssessment parsing, ScoringEngine z nowymi polami, PromptBuilder nowy template

## Kolejność implementacji

1. Domain models (1-3) — foundation
2. ScoringEngine + PromptBuilder (4-5) — flow
3. Persistence (6-7) — storage
4. LLM adapter (8) — parsing
5. REST (9-11) — API
6. Frontend (12-15) — UI
7. Testy (16-17) — walidacja

## Ryzyka

- **Prompt engineering**: LLM może nie zawsze zwracać idealny JSON — potrzebny graceful fallback
- **Rozmiar odpowiedzi**: rozbudowana analiza = dłuższy czas CLI call
- **Backward compatibility**: stare dane bez nowych pól muszą działać
