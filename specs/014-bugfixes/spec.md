# 014 — Bugfixes (BUG-001 + BUG-003)

## Cel

Naprawa dwóch otwartych bugów: (1) N+1 z fetchFiles przy browse, (2) exclude rules ignoruja LLM assessment.
BUG-002 (Claude CLI stdin) juz naprawiony — pomijamy.

## User Stories

### US-1: Lazy loading plikow przy browse (BUG-001)

**Jako** uzytkownik przegladajacy MR
**Chce** zeby browse zwracal wyniki w <3s zamiast 15-20s
**Abym** nie czekal dlugiego czasu na liste PR-ow

**AC:**
- AC-1.1: `fetchMergeRequests()` NIE wywoluje `fetchFiles()` per PR — pliki pobierane tylko w `fetchMergeRequest()` (per-MR analysis)
- AC-1.2: Browse response zawiera metadane PR (tytul, autor, daty, labels) ale `changedFiles` jest pusta lista a `diffStats` pochodzi z PR response (pole `additions`, `deletions`, `changed_files` z GitHub API)
- AC-1.3: Analiza (scoring) nadal pobiera pelne pliki per MR przez `fetchMergeRequest()`
- AC-1.4: Istniejace testy nadal przechodza

### US-2: Soft exclude z LLM override (BUG-003)

**Jako** uzytkownik analizujacy MR z LLM
**Chce** zeby LLM mogl wplynac na verdict nawet jesli regula exclude matchuje (poza label-based)
**Abym** nie widzial sprzecznosci: "LLM: 82% automatable" vs "Score 0.00 NOT_SUITABLE"

**AC:**
- AC-2.1: Exclude rules label-based (hotfix, security, emergency) — zachowuja hard exclude (score=0, verdict=NOT_SUITABLE, LLM pomijane)
- AC-2.2: Exclude rules file-based (minFiles, maxFiles, extensionsOnly) — staja sie soft exclude: duza kara (-0.4) zamiast score=0, LLM moze czesciowo skompensowac
- AC-2.3: Gdy soft exclude matchuje, reasons zawieraja info "soft-exclude" z waga
- AC-2.4: Istniejace BDD scenariusze "hotfix label is excluded" nadal przechodzi (hard exclude zachowany)
- AC-2.5: Nowy BDD scenariusz: PR z 1 plikiem + LLM boost → score > 0 (soft exclude + LLM kompensacja)
