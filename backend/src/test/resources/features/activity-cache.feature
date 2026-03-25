Feature: Cache danych aktywności z incremental update
  As a tech lead
  I want activity data to be cached per repository
  So that switching between contributors doesn't trigger redundant GitHub API calls

  Background:
    Given repozytorium "owner/repo" z wieloma kontrybutorami

  Scenario: Pierwszy request — full fetch i cache
    Given cache aktywności jest pusty
    When pobieram raport aktywności dla repo "owner/repo" i autora "alice"
    Then system wykonuje full fetch z GitHub API
    And dane aktywności są cachowane dla "owner/repo"

  Scenario: Cache hit przy zmianie kontrybutora
    Given dane repo "owner/repo" są w cache aktywności
    When pobieram raport aktywności dla autora "bob"
    Then system nie wykonuje żadnych GitHub API calls
    And raport zawiera dane autora "bob"

  Scenario: Manualny refresh — incremental
    Given dane repo "owner/repo" są w cache aktywności
    And w repo pojawiły się 2 nowe PR-y od ostatniego fetch
    When odświeżam cache dla "owner/repo"
    Then system wykonuje incremental update

  Scenario: Incremental — zero zmian
    Given dane repo "owner/repo" są w cache aktywności
    And w repo nie było żadnych zmian od ostatniego fetch
    When odświeżam cache dla "owner/repo"
    Then system sprawdza zmiany ale nie fetchuje detali

  Scenario: Manualna invalidacja — full refetch
    Given dane repo "owner/repo" są w cache aktywności
    When invaliduję cache dla "owner/repo"
    Then cache jest wyczyszczony
    And następny request wykonuje full fetch
