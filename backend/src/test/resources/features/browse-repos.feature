Feature: Browse merge requests and manage saved repositories
  As a user
  I want to browse MR/PR lists from repositories and manage my saved repos
  So that I can review merge requests before running analysis

  Background:
    Given a repository "owner/repo" with merge requests

  Scenario: User browses MR list from a repository without scoring
    Given the repository has 5 merge requests
    When I browse merge requests for "owner/repo"
    Then the browse result should contain 5 merge requests
    And no merge request should have a score

  Scenario: Browsed repository is automatically saved to the repos list
    Given the repository has 3 merge requests
    When I browse merge requests for "owner/repo"
    Then the saved repos list should contain "owner/repo"

  Scenario: Saved repositories appear in the list after page refresh
    Given the repository has 1 merge request
    When I browse merge requests for "owner/repo"
    And I retrieve all saved repositories
    Then the saved repos list should contain "owner/repo"

  Scenario: User can delete a saved repository from the list
    Given the repository has 1 merge request
    When I browse merge requests for "owner/repo"
    And I delete the saved repository "owner/repo"
    Then the saved repos list should not contain "owner/repo"

  Scenario: User selects a saved repository and browses its MR list
    Given the repository has 4 merge requests
    When I browse merge requests for "owner/repo"
    And I browse merge requests for "owner/repo" again
    Then the browse result should contain 4 merge requests
