# BUG-003: Exclude rules ignoruja LLM assessment — score 0 mimo pozytywnej analizy LLM

## Opis

Gdy exclude rule matchuje (np. byMinChangedFiles, byMaxChangedFiles, byFileExtensionsOnly, byLabels), score jest ustawiany na 0.0 a verdict na NOT_SUITABLE — **niezaleznie od wyniku analizy LLM**.

Efekt: LLM moze powiedziec "82% automatable" ale uzytkownik widzi Score 0.00 / NOT_SUITABLE. Na stronie szczegolow analizy widac sprzecznosc: "Ocena ogolna ~82%" obok "Score 0.00 NOT_SUITABLE".

## Lokalizacja

`ScoringEngine.java:44-52` — branch `if (excluded)` ustawia score=0, verdict=NOT_SUITABLE i pomija LLM assessment.

## Przyczyna

Design decision z MVP: exclude rules mialy byc "hard veto" — jesli PR ma label "hotfix" albo za duzo plikow, nie nadaje sie do automatyzacji niezaleznie od opinii LLM. To ma sens dla labels (hotfix/security), ale nie dla file count rules — LLM moze lepiej ocenic czy duzy PR jest automatyzowalny.

## Proponowane rozwiazanie

Zmienic logike scoringu na:

1. **Hard exclude** (labels: hotfix, security, emergency) — zachowac score=0, verdict=NOT_SUITABLE (LLM nie powinien nadpisywac decyzji bezpieczenstwa)
2. **Soft exclude** (min/max files, file extensions) — zamiast score=0, zastosowac duza kare (-0.5) ale pozwolic LLM na adjustacja. Jesli LLM daje wysoki scoreAdjustment, moze przebic kare.
3. **Konflikt widoczny w UI** — jesli exclude matchuje ale LLM jest pozytywny, pokazac ostrzezenie "Reguly wykluczaja, LLM ocenia pozytywnie — wymagana manualna ocena"

Alternatywnie prostsze podejscie:
- Gdy `useLlm=true`, exclude rules (poza label-based) staja sie penalize rules z duza waga (-0.4)
- LLM scoreAdjustment (+0.5 max) moze je czesciowo skompensowac

## Reprodukcja

1. Przegladaj MR z repo ktore ma PR z 1 zmienionym plikiem lub >50 plikami
2. Wlacz LLM i uruchom analize
3. LLM zwraca overallAutomatability ~80%+ ale score = 0.00, verdict = NOT_SUITABLE
