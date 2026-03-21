Feature: Analysis management
  As a user
  I want each analysis to be independent
  So that I can compare results across multiple analysis runs

  Background:
    Given a repository "owner/repo" with merge requests
    And the repository has 3 merge requests

  Scenario: Multiple analyses for same repo coexist independently
    When I trigger analysis for "owner/repo"
    And I trigger analysis for "owner/repo" again
    Then the two analyses should have different report IDs
    And the provider should have been called twice

  Scenario: New analysis does not overwrite previous analysis
    When I trigger analysis for "owner/repo"
    And I trigger analysis for "owner/repo" again
    Then the analysis history should contain 2 reports for "owner/repo"

  Scenario: User can delete a cached analysis
    When I trigger analysis for "owner/repo"
    And I delete the analysis for "owner/repo"
    Then the analysis for "owner/repo" should not exist

  Scenario: After deleting analysis, re-analysis calculates fresh scores
    When I trigger analysis for "owner/repo"
    And I delete the analysis for "owner/repo"
    And I trigger analysis for "owner/repo" again
    Then the provider should have been called twice

  Scenario: User can select specific MRs for analysis
    When I trigger analysis for "owner/repo" with selected MR ids "1,3"
    Then the selected analysis report should contain 2 results
    And the selected results should only include MR ids "1,3"

  Scenario: Analysis history shows all past analyses with dates and counts
    When I trigger analysis for "owner/repo"
    Then the analysis history should contain a report for "owner/repo"
    And the report should have an analyzed date
    And the report should have 3 total MRs

  Scenario: Analysis history shows individual PRs with title and score
    When I trigger analysis for "owner/repo" with selected MR ids "1,2"
    Then the analysis history should contain 2 PR entries
    And each PR entry should have a title and score

  Scenario: Analysis history can be filtered by repository
    When I trigger analysis for "owner/repo" with selected MR ids "1"
    And I trigger analysis for "other/repo" with selected MR ids "2"
    Then the analysis history should contain reports for 2 different repositories

  Scenario: Analysis with multiple PRs shows separate entries per PR
    When I trigger analysis for "owner/repo" with selected MR ids "1,2,3"
    Then the analysis history should contain 3 PR entries for "owner/repo"
