Feature: Metryki wydajności kontrybutora
  As a tech lead
  I want to see productivity metrics for each contributor
  So that I can assess development velocity and impact

  Background:
    Given repozytorium "owner/repo" z wieloma kontrybutorami

  Scenario: Velocity — PR-y per tydzień
    Given kontrybutor "alice" z 8 zmergowanymi PR-ami w ostatnich 4 tygodniach
    When analizuję aktywność dla metryk "alice"
    Then velocity wynosi 2.0 PR/tydzień

  Scenario: Velocity zero przy braku merged PRs
    Given kontrybutor "alice" bez zmergowanych PR-ów w ostatnich 4 tygodniach
    When analizuję aktywność dla metryk "alice"
    Then velocity wynosi 0.0 PR/tydzień
    And cycle time wyświetla "brak danych"

  Scenario: Trend spadkowy
    Given kontrybutor "alice" z 6 PR-ami w pierwszym tygodniu i 0 w pozostałych trzech
    When analizuję aktywność dla metryk "alice"
    Then velocity trend to "falling"

  Scenario: Cycle time — mediana i p90
    Given kontrybutor "alice" z PR-ami o czasach merge 1h, 2h, 3h, 24h, 48h
    When analizuję aktywność dla metryk "alice"
    Then cycle time median wynosi 3.0 godzin
    And cycle time p90 wynosi 48.0 godzin

  Scenario: Development impact i code churn
    Given kontrybutor "alice" z PR-ami o rozmiarach (100+/50-), (200+/100-), (500+/200-)
    When analizuję aktywność dla metryk "alice"
    Then total impact wynosi 1150 linii
    And churn ratio wynosi 0.44

  Scenario: Review engagement
    Given kontrybutor "alice" dał 8 review i otrzymał 12 review
    When analizuję aktywność dla metryk "alice"
    Then review engagement ratio wynosi 0.67
    And review engagement label to "Zbalansowany"
