# 009 — Performance Fixes

## Cel

Naprawa problemow wydajnosciowych zidentyfikowanych w code review: zbedna paginacja GitHub, sekwencyjne wywolania LLM, nieefektywne zapytania do bazy danych.

## User Stories

### US-1: Efektywna paginacja GitHub
**Jako** uzytkownik przegladajacy MR
**Chce** zeby system pobieratylko tyle stron z GitHub API ile potrzebuje
**Abym** nie marnowat rate limitu i nie czekal zbyt dlugo

**AC:**
- AC-1.1: Paginacja zatrzymuje sie po osiagnieciu limitu z FetchCriteria
- AC-1.2: Limit 5 przy 500 PR w repo pobiera maksymalnie 1 strone (per_page >= limit)

### US-2: Rownolegle wywolania LLM
**Jako** uzytkownik analizujacy MR z LLM
**Chce** zeby analiza wielu MR odbywala sie rownolegle
**Abym** nie czekal 10 minut na 10 MR

**AC:**
- AC-2.1: Analiza LLM wielu MR odbywa sie rownolegle (max 3 wspolbiezne)
- AC-2.2: Timeout per MR nadal dziala (60s)
- AC-2.3: Blad jednego MR nie blokuje analizy pozostalych

### US-3: Efektywne zapytania do bazy
**Jako** uzytkownik API
**Chce** zeby endpointy pobieraly tylko potrzebne dane z DB
**Abym** dostal szybka odpowiedz

**AC:**
- AC-3.1: GET /api/analysis/{reportId}/mrs/{resultId} — dedykowane query zamiast ladowania calego raportu
- AC-3.2: GET /api/analysis?projectSlug=X — query z filtrem zamiast ladowania wszystkiego i filtrowania w pamieci
