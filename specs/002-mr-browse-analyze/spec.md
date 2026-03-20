# Feature Specification: Przegladanie MR/PR i zarzadzanie analizami

**Feature Branch**: `002-mr-browse-analyze`
**Created**: 2026-03-20
**Status**: Draft
**Input**: Dwuetapowy flow: najpierw wybor repo i przegladanie MR/PR, potem analiza. Zapamietywanie repo, zapisywanie analiz, podglad szczegolow MR w nowej karcie.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Wybor repozytorium i przegladanie MR/PR (Priority: P1)

Jako uzytkownik chce najpierw wybrac repozytorium i zobaczyc liste MR/PR, zanim uruchomie analize — abym mogl przejrzec co jest w repo i zdecydowac czy chce analizowac.

**Why this priority**: Zmienia fundamentalny flow aplikacji z "od razu analizuj" na "najpierw przegladaj, potem analizuj". Bez tego kolejne stories nie maja sensu.

**Independent Test**: Uzytkownik wpisuje slug repo, klika "Pobierz MR" i widzi liste MR/PR z podstawowymi danymi (tytul, autor, data, status) BEZ scoringu.

**Acceptance Scenarios**:

1. **Given** uzytkownik otwiera dashboard, **When** wpisuje "owner/repo" i klika "Pobierz MR", **Then** system wyswietla liste MR/PR z kolumnami: numer, tytul, autor, data utworzenia, status (merged/open/closed), liczba zmienionych plikow
2. **Given** lista MR/PR jest wyswietlona, **When** uzytkownik klika przycisk "Analizuj", **Then** system uruchamia scoring i wyswietla wyniki z kolumnami score i verdict obok istniejacych danych
3. **Given** lista MR/PR jest wyswietlona, **When** uzytkownik klika "Analizuj" ponownie, **Then** system uzywa zapisanej analizy zamiast liczyc od nowa (jesli istnieje dla tego repo i zakresu dat)

---

### User Story 2 - Zapamietywanie repozytoriow (Priority: P1)

Jako uzytkownik chce aby system zapamietal repozytoria ktore juz analizowalem, abym mogl szybko wybrac je z listy rozwijalnej zamiast wpisywac slug recznie.

**Why this priority**: Podstawowa wygoda UX — uzytkownik nie chce wpisywac tego samego sluga wiele razy.

**Independent Test**: Uzytkownik wpisuje repo, pobiera MR. Nastepnym razem widzi to repo w dropdown i moze je wybrac jednym kliknieciem.

**Acceptance Scenarios**:

1. **Given** uzytkownik wpisuje "owner/repo" i pobiera MR, **When** wraca na dashboard, **Then** "owner/repo" pojawia sie w dropdown listy zapisanych repozytoriow
2. **Given** dropdown zawiera 3 zapisane repozytoria, **When** uzytkownik wybiera jedno z listy, **Then** pole project slug wypelnia sie automatycznie
3. **Given** uzytkownik chce dodac nowe repo, **When** wpisuje nowy slug recznie, **Then** po pobraniu MR nowe repo jest dodane do dropdown
4. **Given** uzytkownik chce usunac repo z listy, **When** klika ikone "X" obok repo w dropdown, **Then** repo znika z listy

---

### User Story 3 - Zapisywanie i zarzadzanie analizami (Priority: P1)

Jako uzytkownik chce aby wyniki analiz byly zapisywane i dostepne bez ponownego uruchamiania, abym mogl porownywac wyniki po zmianach w regulach. Chce tez moc usunac zapisana analize zeby wymusic ponowne obliczenie.

**Why this priority**: Kluczowe do iterowania nad regulami — analizujesz, zmieniasz reguly, usuwasz stara analize, analizujesz ponownie, porownujesz.

**Independent Test**: Uruchom analize, zamknij przegladarke, otworz ponownie — wyniki sa. Kliknij "Usun analize" — wyniki znikaja, mozna analizowac od nowa.

**Acceptance Scenarios**:

1. **Given** analiza zostala uruchomiona dla "owner/repo", **When** uzytkownik odswiezy strone, **Then** wyniki analizy sa nadal dostepne (zaladowane z bazy)
2. **Given** analiza istnieje dla "owner/repo", **When** uzytkownik klika "Analizuj" ponownie, **Then** system wyswietla zapisane wyniki zamiast liczyc od nowa
3. **Given** analiza istnieje, **When** uzytkownik klika przycisk "Usun analize", **Then** wyniki sa usuwane z bazy i uzytkownik moze uruchomic analize ponownie
4. **Given** lista analiz na dashboardzie, **When** uzytkownik widzi historie, **Then** kazda analiza ma date, repo slug, liczbe MR i przycisk "Usun"

---

### User Story 4 - Podglad szczegolow MR/PR w nowej karcie (Priority: P2)

Jako uzytkownik chce kliknac na MR/PR i zobaczyc pelne szczegoly (dane z GitHub/GitLab) w nowej karcie przegladarki, abym mogl przejrzec zawartosc MR bez opuszczania dashboardu.

**Why this priority**: Uzupelnia flow przegladania — uzytkownik widzi liste, klika na interesujacy MR i widzi szczegoly. P2 bo lista dziala bez tego.

**Independent Test**: Kliknij na MR w tabeli — otwiera sie nowa karta z pelnym podsumowaniem MR: tytul, opis, autor, branch, diff stats, lista zmienionych plikow, labels, score breakdown (jesli analiza zostala uruchomiona).

**Acceptance Scenarios**:

1. **Given** lista MR/PR jest wyswietlona, **When** uzytkownik klika na wiersz MR, **Then** otwiera sie nowa karta przegladarki ze strona szczegolow MR
2. **Given** strona szczegolow MR, **Then** wyswietla: tytul, opis (renderowany markdown), autor, branch source→target, daty (utworzenia, merge), labels, diff stats (additions/deletions/files count), liste zmienionych plikow z ich statusem
3. **Given** MR zostal przeanalizowany (ma score), **Then** strona szczegolow wyswietla dodatkowo: score, verdict badge, score breakdown (reguly + wagi), komentarz LLM
4. **Given** MR nie zostal jeszcze przeanalizowany, **Then** strona szczegolow wyswietla dane MR bez sekcji scoring (sekcja scoring jest ukryta)
5. **Given** strona szczegolow MR, **Then** wyswietla link do oryginalnego MR/PR na GitHub/GitLab (otwierany w nowej karcie)

---

### Edge Cases

- Co jesli repo zostalo usuniete z GitHub miedzy zapisaniem a ponownym otwarciem? System wyswietla komunikat bledu i oferuje usuniecie z listy.
- Co jesli analiza zostala zapisana ze starymi regulami? Uzytkownik moze ja usunac i uruchomic ponownie z nowymi regulami.
- Co jesli MR zostal zamkniety/zmergowany miedzy pobraniem a podgladem szczegolow? System wyswietla aktualne dane z API.
- Co jesli lista MR jest pusta (repo bez PR)? Dashboard wyswietla komunikat "Brak MR/PR w wybranym zakresie".
- Co jesli opis MR zawiera markdown z obrazkami? System renderuje markdown ale pomija obrazki.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST wyswietlac dwuetapowy flow: najpierw "Pobierz MR" (lista bez scoringu), potem "Analizuj" (scoring)
- **FR-002**: System MUST zapamietywac project slugi w bazie danych i wyswietlac je w dropdown
- **FR-003**: System MUST umozliwiac dodawanie i usuwanie repozytoriow z listy zapisanych
- **FR-004**: System MUST zapisywac wyniki analiz w bazie danych (persystentnie)
- **FR-005**: System MUST wykrywac istniejaca analize i uzyc zapisanych wynikow zamiast liczyc ponownie
- **FR-006**: System MUST umozliwiac usuwanie zapisanej analizy (z potwierdzeniem)
- **FR-007**: System MUST wyswietlac historie analiz z data, repo, liczba MR i przyciskiem usun
- **FR-008**: System MUST otwierac szczegoly MR/PR w nowej karcie przegladarki (target="_blank")
- **FR-009**: System MUST pobierac pelne dane MR z GitHub/GitLab API (tytul, opis, diff, pliki, labels, daty)
- **FR-010**: System MUST wyswietlac score breakdown na stronie szczegolow jesli analiza zostala uruchomiona
- **FR-011**: System MUST ukrywac sekcje scoring na stronie szczegolow jesli analiza nie zostala uruchomiona
- **FR-012**: System MUST wyswietlac link do oryginalnego MR/PR na GitHub/GitLab
- **FR-013**: System MUST miec testy BDD pokrywajace nowe scenariusze
- **FR-014**: System MUST miec testy jednostkowe dla nowej logiki (repo management, analysis caching)

### Key Entities

- **SavedRepository**: Zapisane repozytorium — project slug, provider, data dodania, ostatnia analiza
- **AnalysisCache**: Zapisana analiza powiazana z repo + zakresem dat — umozliwia wykrycie czy analiza juz istnieje

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uzytkownik moze przegladac liste MR/PR bez uruchamiania analizy (dwuetapowy flow)
- **SC-002**: Zapisane repozytoria pojawiaja sie w dropdown po odswiezeniu strony
- **SC-003**: Zapisana analiza jest ladowana natychmiast (bez ponownego wywolania GitHub API)
- **SC-004**: Usuniecie analizy umozliwia ponowne uruchomienie z nowymi regulami
- **SC-005**: Klikniecie na MR otwiera nowa karte z pelnymi szczegolami (min. tytul, opis, autor, diff stats, pliki, labels)
- **SC-006**: Testy BDD pokrywaja nowe scenariusze (min. 8 nowych scenariuszy .feature)
- **SC-007**: Testy jednostkowe dla repo management i analysis caching (min. 10 nowych test cases)
