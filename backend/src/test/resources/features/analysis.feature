Feature: End-to-end merge request analysis
  As a development team lead
  I want to analyze a repository and get scored results for each PR
  So that I can identify which PRs are candidates for LLM automation

  Background:
    Given a repository "owner/repo" with merge requests

  Scenario: Running analysis on a repository returns scored results
    Given the repository has 3 merge requests
    When I trigger analysis for "owner/repo"
    Then the analysis report should contain 3 results
    And the report project slug should be "owner/repo"

  Scenario: Each analyzed PR receives a score and verdict
    Given the repository has 2 merge requests
    When I trigger analysis for "owner/repo"
    Then every result should have a score between 0.0 and 1.0
    And every result should have a verdict of AUTOMATABLE, MAYBE, or NOT_SUITABLE

  Scenario: Analysis results include score breakdown details
    Given the repository has 1 merge request
    When I trigger analysis for "owner/repo"
    Then every result should have a non-empty list of reasons
    And every result should have a non-empty list of matched rules

  Scenario: Analysis of non-existent repository returns an error
    Given the provider will fail with "repository not found"
    When I trigger analysis for "invalid/repo"
    Then the system should return an error response

  Scenario: Analysis with LLM enabled includes LLM assessment comment
    Given the repository has 1 merge request
    And the LLM analyzer returns a comment "Suitable for automation"
    When I trigger analysis for "owner/repo" with LLM enabled
    Then every result should have an LLM comment containing "Suitable for automation"

  Scenario: Analysis with LLM disabled has no LLM comment
    Given the repository has 1 merge request
    When I trigger analysis for "owner/repo" with LLM disabled
    Then every result should have no LLM comment

  Scenario: Analysis continues when LLM times out
    Given the repository has 1 merge request
    And the LLM analyzer times out
    When I trigger analysis for "owner/repo" with LLM enabled
    Then the analysis report should contain 1 results
    And every result should have an LLM comment containing "LLM error"

  Scenario: Analysis with blank project slug is rejected
    When I trigger analysis with a blank project slug
    Then the system should return a validation error about project slug

  Scenario: Retrieving analysis report loads results efficiently
    Given the repository has 5 merge requests
    When I trigger analysis for "owner/repo"
    And I retrieve the analysis report by its ID
    Then the analysis report should contain 5 results
    And each result should have a score and verdict
