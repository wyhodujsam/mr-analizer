# Feature Specification: Cache dla pobranych PR

**Feature Branch**: `006-browse-cache`
**Created**: 2026-03-21
**Status**: Draft

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cache browse wynikow (Priority: P1)

Jako uzytkownik chce aby lista PR po kliknieciu "Pobierz MR" byla cachowana, abym nie czekal na ponowne pobranie z GitHub API gdy wracam do tego samego repo.

**Acceptance Scenarios**:

1. **Given** uzytkownik pobrał MR z "owner/repo", **When** klika "Pobierz MR" ponownie dla tego samego repo, **Then** lista laduje sie natychmiast z cache (bez zapytania do GitHub API)
2. **Given** cache istnieje dla "owner/repo", **When** uzytkownik klika przycisk "Odswiez", **Then** system pobiera dane z GitHub API na nowo i aktualizuje cache
3. **Given** cache istnieje, **When** uzytkownik zmienia zakres dat lub limit, **Then** system pobiera dane na nowo (cache jest per slug+criteria)

## Requirements *(mandatory)*

- **FR-001**: System MUST cachowac wyniki browse per projectSlug
- **FR-002**: System MUST wyswietlac przycisk "Odswiez" obok "Pobierz MR" gdy cache istnieje
- **FR-003**: Przycisk "Odswiez" MUST invalidowac cache i pobierac dane na nowo z API
- **FR-004**: Testy BDD (min. 2 scenariusze)

## Success Criteria

- **SC-001**: Drugie klikniecie "Pobierz MR" laduje dane natychmiast z cache
- **SC-002**: Przycisk "Odswiez" wymusza pobranie z API
