Feature: Merge request scoring and verdict assignment
  As a development team lead
  I want merge requests to be automatically scored for automation potential
  So that I can prioritize which MRs to automate first

  Background:
    Given the scoring engine is configured with default thresholds

  Scenario: PR with hotfix label is excluded from automation
    Given a merge request with label "hotfix"
    When the scoring engine evaluates it
    Then the score should be 0
    And the verdict should be NOT_SUITABLE
    And the reasons should contain "excluded by label"

  Scenario: Refactoring PR with tests scores high for automation
    Given a merge request with title "refactor: extract payment service"
    And the merge request has 5 changed files
    And the merge request has tests
    When the scoring engine evaluates it
    Then the score should be greater than 0.7
    And the verdict should be AUTOMATABLE

  Scenario: Large diff without description is penalized
    Given a merge request with 600 lines of diff
    And the merge request has no description
    When the scoring engine evaluates it
    Then the score should be less than 0.5

  Scenario: PR with too few changed files is excluded
    Given a merge request with 1 changed file
    When the scoring engine evaluates it with a minimum 2 files rule
    Then the score should be 0
    And the verdict should be NOT_SUITABLE

  Scenario: PR with only config file extensions is excluded
    Given a merge request with only ".yml" and ".toml" files
    When the scoring engine evaluates it with a config-only exclusion rule
    Then the score should be 0
    And the verdict should be NOT_SUITABLE

  Scenario: Multiple boosts lead to automatable verdict
    Given a merge request with title "refactor: simplify validation"
    And the merge request has 5 changed files
    And the merge request has tests
    And the merge request has label "tech-debt"
    And the merge request has a description "Clean up validation logic and add unit tests"
    When the scoring engine evaluates it with boost rules
    Then the score should be greater than 0.7
    And the verdict should be AUTOMATABLE

  Scenario: Multiple penalties lead to lower verdict
    Given a merge request with 600 lines of diff
    And the merge request has no description
    And the merge request touches config files
    When the scoring engine evaluates it with penalty rules
    Then the verdict should be MAYBE or NOT_SUITABLE

  Scenario: PR with too few files but positive LLM gets partial score (soft exclude)
    Given a merge request with 1 changed file
    And the LLM assessment has score adjustment of 0.3
    When the scoring engine evaluates it with a minimum 2 files rule and LLM
    Then the score should be greater than 0
    And the verdict should not be NOT_SUITABLE from hard exclude

  Scenario: PR matching no rules gets base score
    Given a merge request with title "update readme"
    And the merge request has 3 changed files
    And the merge request has 100 lines of diff
    When the scoring engine evaluates it with no matching rules
    Then the score should be 0.5
    And the verdict should be MAYBE
