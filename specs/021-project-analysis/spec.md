# 021: Project Analysis — AI Potential + BDD + SDD per PR

**Feature Branch**: `021-project-analysis`
**Created**: 2026-03-25
**Status**: Draft
**Input**: Nowa strona analizująca WSZYSTKIE PR-y repozytorium i zestawiająca trzy wymiary: potencjał automatyzacji AI, obecność BDD, obecność SDD. Ze szczegółowym breakdown i drill-down do poszczególnych PR-ów.

## Problem

Zarząd potrzebuje widoku całego projektu — nie pojedynczego PR, ale **zestawienia wszystkich PR-ów** w trzech wymiarach:

1. **Potential for AI usage** — ile PR-ów mogłoby być zautomatyzowanych przez LLM? Skąd się bierze potencjał (jakie reguły, jaki rozkład score'ów)? Korzystamy z istniejącej funkcjonalności scoringu (ScoringEngine + opcjonalnie LLM).
2. **BDD created** — ile PR-ów zawiera artefakty BDD (pliki `.feature`, step definitions)? Czy zespół stosuje Behavior Driven Development?
3. **SDD created** — ile PR-ów zawiera artefakty SDD/Spec Kit (spec.md, plan.md, tasks.md)? Czy zespół stosuje Specification Driven Development?

Potrzebny jest zarówno **widok zagregowany** (karty z %, wykresy) jak i **drill-down** do analizy poszczególnych PR-ów (skąd wynik, jakie reguły zadziałały, jakie pliki BDD/SDD).

## User Scenarios & Testing

### User Story 1 — Analiza całego projektu (Priority: P1)

Użytkownik (zarząd / engineering manager) wybiera repozytorium i uruchamia analizę całego projektu. System analizuje WSZYSTKIE zamknięte/zmergowane PR-y i prezentuje zagregowany raport.

**Acceptance Scenarios**:

1. **Given** repozytorium z 50 PR-ami, **When** użytkownik uruchamia analizę projektu, **Then** system analizuje wszystkie PR-y i wyświetla raport z trzema sekcjami: AI Potential, BDD, SDD.
2. **Given** analiza projektu w toku, **When** trwa przetwarzanie, **Then** użytkownik widzi spinner z informacją "Analizuję X PR-ów...".
3. **Given** zakończona analiza, **When** użytkownik przegląda raport, **Then** widzi tabelę z każdym PR-em i kolumnami: tytuł, autor, data, AI score, AI verdict, BDD, SDD.

---

### User Story 2 — AI Potential — szczegółowy breakdown (Priority: P1)

Dashboard wyświetla nie tylko % AUTOMATABLE, ale również **skąd się bierze wynik**: rozkład score'ów, najczęstsze reguły boost/penalize, histogram.

**Acceptance Scenarios**:

1. **Given** zakończona analiza 50 PR-ów, **When** dashboard się ładuje, **Then** widać kartę "AI Potential" z:
   - Donut chart: % AUTOMATABLE / MAYBE / NOT_SUITABLE
   - Średni score projektu
   - Liczba PR-ów per verdict
2. **Given** zakończona analiza, **When** patrzę na sekcję AI Potential, **Then** widać "Top reguły" — lista najczęściej matchujących reguł z liczbą PR-ów (np. "boost:hasTests — 35 PR-ów", "penalize:largeDiff — 12 PR-ów").
3. **Given** zakończona analiza, **When** patrzę na sekcję AI Potential, **Then** widać histogram rozkładu score'ów (np. 0.0-0.2: 5 PR, 0.2-0.4: 8 PR, 0.4-0.6: 15 PR, 0.6-0.8: 18 PR, 0.8-1.0: 4 PR).

---

### User Story 3 — BDD Detection per PR (Priority: P1)

System sprawdza czy PR zawiera artefakty BDD — pliki `.feature` (Gherkin), step definitions, lub inne konfigurowalne wzorce.

**Acceptance Scenarios**:

1. **Given** PR dodający plik `*.feature`, **When** system sprawdza BDD, **Then** PR oznaczony jako "BDD: tak".
2. **Given** PR dodający plik `*Steps.java` lub `*_steps.py`, **When** system sprawdza BDD, **Then** PR oznaczony jako "BDD: tak".
3. **Given** PR bez żadnych plików BDD, **When** system sprawdza BDD, **Then** PR oznaczony jako "BDD: nie".
4. **Given** konfiguracja wzorców BDD w `application.yml`, **When** użytkownik zmieni wzorce, **Then** system stosuje nowe wzorce.
5. **Given** zakończona analiza, **When** patrzę na kartę BDD, **Then** widać % PR-ów z BDD + liczbę.

---

### User Story 4 — SDD Detection per PR (Priority: P1)

System sprawdza czy PR zawiera artefakty SDD/Spec Kit — spec.md, plan.md, tasks.md, lub inne konfigurowalne wzorce.

**Acceptance Scenarios**:

1. **Given** PR dodający `specs/*/spec.md`, **When** system sprawdza SDD, **Then** PR oznaczony jako "SDD: tak".
2. **Given** PR dodający `plan.md` i `tasks.md`, **When** system sprawdza SDD, **Then** PR oznaczony jako "SDD: tak".
3. **Given** PR z kodem bez artefaktów specyfikacji, **When** system sprawdza SDD, **Then** PR oznaczony jako "SDD: nie".
4. **Given** konfiguracja wzorców SDD w `application.yml`, **When** użytkownik zmieni wzorce, **Then** system stosuje nowe wzorce.
5. **Given** zakończona analiza, **When** patrzę na kartę SDD, **Then** widać % PR-ów z SDD + liczbę.

---

### User Story 5 — Drill-down: szczegóły analizy PR (Priority: P1)

Użytkownik klika wiersz PR w tabeli i widzi szczegółową analizę tego PR-a: score breakdown (jakie reguły, ile punktów), listę plików BDD/SDD, dane PR-a.

**Acceptance Scenarios**:

1. **Given** tabela z wynikami, **When** użytkownik klika wiersz PR, **Then** rozwija się panel z detalami:
   - Score breakdown: tabela reguł (nazwa, boost/penalize, waga, czy matched)
   - Pliki BDD: lista znalezionych plików `.feature` / `*Steps.java` itd.
   - Pliki SDD: lista znalezionych plików `spec.md` / `plan.md` itd.
   - Dane PR: autor, branch, data, rozmiar (additions/deletions), opis
2. **Given** PR z verdict AUTOMATABLE, **When** patrzę na breakdown, **Then** widzę które reguły boost dały wysoki score.
3. **Given** PR z verdict NOT_SUITABLE, **When** patrzę na breakdown, **Then** widzę które reguły penalize/exclude obniżyły score.

---

### User Story 6 — Zagregowany dashboard z filtrowaniem i sortowaniem (Priority: P1)

Dashboard wyświetla trzy karty podsumowujące + tabelę z filtrowaniem i sortowaniem.

**Acceptance Scenarios**:

1. **Given** dashboard, **When** użytkownik klika nagłówek kolumny "AI Score", **Then** tabela sortuje się po score (asc/desc toggle).
2. **Given** dashboard, **When** użytkownik filtruje po "Verdict: AUTOMATABLE", **Then** tabela pokazuje tylko PR-y z verdict AUTOMATABLE.
3. **Given** dashboard, **When** użytkownik filtruje po "BDD: tak", **Then** tabela pokazuje tylko PR-y z BDD.
4. **Given** dashboard, **When** użytkownik filtruje po "SDD: tak" + "Verdict: AUTOMATABLE", **Then** tabela pokazuje PR-y spełniające OBA kryteria.

## Scope & Constraints

### In scope
- Nowa strona `/project` z analizą całego repozytorium
- AI Potential: ScoringEngine (reguły), LLM opcjonalnie
- AI Potential: szczegółowy breakdown — donut chart, histogram score, top reguły, średni score
- BDD/SDD detection: konfigurowalne wzorce plików w `application.yml`
- Drill-down: kliknięcie PR → rozwinięcie z score breakdown, pliki BDD/SDD, dane PR
- Tabela z sortowaniem (per kolumna) i filtrowaniem (verdict, BDD, SDD)
- Karty podsumowujące z % i liczbami
- Cache: korzystanie z activity cache (PR-y + detale)

### Out of scope
- Persystencja wyników analizy projektowej (in-memory, on-demand)
- Porównywanie projektów (future)
- Trend w czasie (wymaga persystencji)
- Eksport do PDF/CSV (future)

## Technical Notes

- BDD/SDD detection wymaga listy plików per PR — `GET /pulls/{number}/files` (osobny call per PR)
- Korzystamy z activity cache (020) — detale PR-ów już cachowane, files NIE (za dużo pamięci)
- Wzorce BDD/SDD konfigurowalne w `application.yml` — glob-style matching na `ChangedFile.path`
- Domyślne wzorce:
  - BDD: `*.feature`, `*Steps.java`, `*_steps.py`, `*_steps.rb`, `*.steps.ts`, `*Steps.kt`
  - SDD: `spec.md`, `plan.md`, `tasks.md`, `research.md`, `quickstart.md`, `checklist.md`
- AI scoring: per-PR, korzysta z ScoringEngine (reguły z application.yml). LLM domyślnie OFF (za drogi na 100+ PRs)
- Score breakdown: ScoringEngine zwraca `List<RuleResult>` per PR — wystarczy do szczegółów
- Drill-down: expandable row w tabeli (nie osobna strona) — mniej nawigacji, szybszy wgląd
- Top reguły: agregacja `RuleResult.ruleName()` across all PRs → count per rule
- Histogram: buckety score 0.0-0.2, 0.2-0.4, ..., 0.8-1.0 → count per bucket
