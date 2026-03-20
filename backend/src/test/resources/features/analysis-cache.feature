Feature: Analysis caching and management
  As a user
  I want analysis results to be cached and manageable
  So that I can avoid redundant analysis runs and re-analyze with updated rules

  Background:
    Given a repository "owner/repo" with merge requests
    And the repository has 3 merge requests

  Scenario: Analysis results are cached and returned instantly on second request
    When I trigger analysis for "owner/repo"
    And I trigger analysis for "owner/repo" again
    Then the second analysis should return the cached report
    And the provider should have been called only once

  Scenario: User can delete a cached analysis
    When I trigger analysis for "owner/repo"
    And I delete the analysis for "owner/repo"
    Then the analysis for "owner/repo" should not exist

  Scenario: After deleting analysis, re-analysis calculates fresh scores
    When I trigger analysis for "owner/repo"
    And I delete the analysis for "owner/repo"
    And I trigger analysis for "owner/repo" again
    Then the provider should have been called twice

  Scenario: Analysis history shows all past analyses with dates and counts
    When I trigger analysis for "owner/repo"
    Then the analysis history should contain a report for "owner/repo"
    And the report should have an analyzed date
    And the report should have 3 total MRs
