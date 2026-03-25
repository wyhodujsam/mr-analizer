Feature: Analiza projektu — AI Potential + BDD + SDD
  As an engineering manager
  I want to analyze all PRs in a repository
  So that I can see AI automation potential, BDD adoption, and SDD adoption

  Background:
    Given repozytorium "owner/repo" z wieloma kontrybutorami

  Scenario: Analiza projektu z AI scoring i BDD/SDD detection
    Given repozytorium z 5 PR-ami o różnych cechach
    When analizuję projekt "owner/repo"
    Then otrzymuję wynik z 5 wierszami
    And summary zawiera poprawne procenty AI/BDD/SDD

  Scenario: AI Potential — scoring z regułami
    Given PR z testami i małymi zmianami
    And PR z dużymi zmianami bez opisu
    When analizuję projekt "owner/repo"
    Then PR z testami ma verdict AUTOMATABLE lub MAYBE
    And PR z dużymi zmianami ma niższy score

  Scenario: BDD detection — pliki .feature
    Given PR dodający plik "src/test/features/login.feature"
    And PR bez plików BDD
    When analizuję projekt "owner/repo"
    Then pierwszy PR ma hasBdd true
    And drugi PR ma hasBdd false

  Scenario: SDD detection — pliki spec-kit
    Given PR dodający plik "specs/005/spec.md"
    And PR bez plików SDD
    When analizuję projekt "owner/repo"
    Then pierwszy PR ma hasSdd true
    And drugi PR ma hasSdd false

  Scenario: Drill-down — score breakdown per PR
    Given repozytorium z 3 PR-ami
    When analizuję projekt "owner/repo"
    Then każdy wiersz zawiera listę ruleResults z nazwą i wagą

  Scenario: Summary — top reguły i histogram
    Given repozytorium z 5 PR-ami o różnych cechach
    When analizuję projekt "owner/repo"
    Then summary zawiera topRules z liczbą matchów
    And summary zawiera histogram z 5 bucketami
