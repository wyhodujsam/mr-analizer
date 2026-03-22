Feature: VCS provider merge request fetching
  As the system
  I want to fetch and filter merge requests from a VCS provider
  So that only relevant PRs are passed to the scoring engine

  Scenario: Fetching PRs from a repository returns merge request data
    Given a provider returning 3 merge requests for "owner/repo"
    When the system fetches merge requests for "owner/repo"
    Then 3 merge requests should be returned
    And each merge request should have a title, author, and external ID

  Scenario: PR with no changed files is excluded from results
    Given a provider returning merge requests where one has no changed files
    When the system fetches and filters merge requests
    Then the result should not contain the merge request with no changed files

  Scenario: Repository rate limit returns a clear error message
    Given a provider that responds with rate limit exceeded
    When the system attempts to fetch merge requests
    Then the system should return a rate limit error with a clear message

  Scenario: Provider without authentication token returns auth error immediately
    Given a provider that has no authentication token configured
    When the system attempts to fetch merge requests
    Then the system should return an authentication error with message about missing token
