Feature: Detailed LLM analysis with structured assessment
  As a development team lead
  I want detailed LLM analysis with categories, human oversight areas, and summary
  So that I can understand WHY a PR is suitable for LLM automation

  Background:
    Given a repository "owner/repo" with merge requests

  Scenario: LLM returns detailed analysis with categories and oversight areas
    Given the repository has 1 merge request
    And the LLM analyzer returns a detailed assessment with overall automatability 90
    When I trigger analysis for "owner/repo" with LLM enabled
    Then the analysis report should contain 1 results
    And every result should have overall automatability of 90
    And every result should have at least 1 analysis category
    And every result should have at least 1 human oversight item
    And every result should have at least 1 LLM-friendly reason
    And every result should have at least 1 summary aspect

  Scenario: Analysis without LLM has no detailed assessment data
    Given the repository has 1 merge request
    When I trigger analysis for "owner/repo" with LLM disabled
    Then every result should have overall automatability of 0
    And every result should have 0 analysis categories
    And every result should have 0 human oversight items

  Scenario: LLM returns partial data — missing optional fields gracefully handled
    Given the repository has 1 merge request
    And the LLM analyzer returns assessment with only score and comment
    When I trigger analysis for "owner/repo" with LLM enabled
    Then the analysis report should contain 1 results
    And every result should have an LLM comment containing "partial"
    And every result should have 0 analysis categories
    And every result should have 0 human oversight items
    And every result should have overall automatability of 0
