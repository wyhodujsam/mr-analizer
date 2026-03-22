Feature: Analiza aktywności kontrybutora w repozytorium
  As a tech lead
  I want to analyze contributor activity and detect anomalies
  So that I can monitor development process health

  Background:
    Given repozytorium "owner/repo" z wieloma kontrybutorami

  Scenario: Dashboard z metrykami i flagami
    Given kontrybutor "jan.kowalski" z 15 PR-ami
    And 3 PR-y mają ponad 500 linii zmian
    And 1 PR został zmergowany w 3 minuty z 200 liniami zmian
    When analizuję aktywność "jan.kowalski"
    Then widzę 15 PR-ów w statystykach
    And widzę 3 flagi "Za duży PR" z severity warning
    And widzę 1 flagę "Podejrzanie szybki merge" z severity critical

  Scenario: Praca w weekend
    Given kontrybutor "anna.nowak" z 10 PR-ami
    And 4 PR-y zostały utworzone w sobotę lub niedzielę
    When analizuję aktywność "anna.nowak"
    Then widzę 4 flagi "Praca w weekend" z severity info
    And widzę flagę "Wysoki odsetek pracy weekendowej" z severity warning

  Scenario: Brak nieprawidłowości
    Given kontrybutor "piotr.zielinski" z 8 PR-ami bez nieprawidłowości
    When analizuję aktywność "piotr.zielinski"
    Then widzę 8 PR-ów w statystykach
    And sekcja flag jest pusta

  Scenario: Brak review na PR
    Given kontrybutor "jan.kowalski" z PR numer 42 bez reviews
    When analizuję aktywność "jan.kowalski"
    Then widzę flagę "Brak review" z severity warning na PR 42

  Scenario: Brak aktywności
    Given kontrybutor "ghost" bez żadnych PR-ów
    When analizuję aktywność "ghost"
    Then widzę komunikat braku aktywności

  Scenario: Pobranie listy kontrybutorów
    Given repozytorium "owner/repo" z PR-ami od 3 różnych autorów
    When pobieram listę kontrybutorów
    Then otrzymuję 3 kontrybutorów z liczbą PR-ów

  Scenario: Heatmapa aktywności
    Given kontrybutor "jan.kowalski" z PR-ami w dniach "2026-03-02" i "2026-03-02" i "2026-03-05"
    When analizuję aktywność "jan.kowalski"
    Then heatmapa pokazuje 2 PR-y w dniu "2026-03-02"
    And heatmapa pokazuje 1 PR w dniu "2026-03-05"
