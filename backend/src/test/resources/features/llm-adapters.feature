Feature: LLM API adaptery — OpenAI-compatible i Anthropic Messages
  As a developer
  I want to switch between LLM providers via configuration
  So that I can use different LLM backends for MR analysis

  Scenario: OpenAI-compatible API — analiza PR
    Given LLM adapter ustawiony na "openai-api"
    And OpenAI API zwraca poprawny response z score 0.15 i komentarzem "looks good"
    When system analizuje PR z LLM
    Then LlmAssessment zawiera scoreAdjustment 0.15
    And LlmAssessment zawiera comment "looks good"
    And LlmAssessment provider to "openai-api"

  Scenario: Anthropic Messages API — analiza PR
    Given LLM adapter ustawiony na "anthropic-api"
    And Anthropic API zwraca poprawny response z score 0.1 i komentarzem "doable"
    When system analizuje PR z LLM
    Then LlmAssessment zawiera scoreAdjustment 0.1
    And LlmAssessment zawiera comment "doable"
    And LlmAssessment provider to "anthropic-api"

  Scenario: Wspólny parser response — JSON ze wszystkimi polami
    Given response JSON z scoreAdjustment, comment, categories, humanOversightRequired
    When parser przetwarza response
    Then LlmAssessment zawiera pełne dane (categories, oversight, summaryTable)

  Scenario: Timeout API — graceful fallback
    Given LLM API nie odpowiada w czasie
    When system analizuje PR z LLM
    Then LlmAssessment zawiera score 0.0 i comment z informacją o timeout

  Scenario: Błąd autoryzacji API
    Given LLM API zwraca 401 Unauthorized
    When system analizuje PR z LLM
    Then LlmAssessment zawiera score 0.0 i comment z informacją o błędzie auth
