Feature: Scoring handles MR with missing data
  As a user analyzing merge requests
  I want scoring to work even when MR has no labels, diffStats, or changedFiles
  So that the analysis never crashes with NullPointerException

  Background:
    Given the scoring engine is configured with default thresholds

  Scenario: MR without labels does not crash on label exclusion rule
    Given a merge request with no labels
    And the merge request has 5 changed files
    When the scoring engine evaluates it with label exclusion rules
    Then the score should be greater than 0
    And the verdict should not be null

  Scenario: MR without diffStats does not crash on file count rules
    Given a merge request with no diff stats
    When the scoring engine evaluates it with a minimum 2 files rule
    Then the score should be 0
    And the verdict should be NOT_SUITABLE

  Scenario: MR without changedFiles does not crash on config touch rule
    Given a merge request with no changed files
    When the scoring engine evaluates it with penalty rules
    Then the verdict should not be null

  Scenario: MR with all null optional fields gets base score
    Given a merge request with no labels
    And the merge request has no diff stats
    And the merge request has no changed files
    When the scoring engine evaluates it with no matching rules
    Then the score should be 0.5
    And the verdict should be MAYBE
